package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.dto.request.InvitationCreateRequestDto;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import com.firstpages.domain.invitation.scheduler.InvitationPhotoCleanupJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 MySQL에서 청첩장 삭제와 아웃박스 등록의 트랜잭션 경계를 검증한다.
@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "RUN_INVITATION_DELETE_MYSQL_TEST",
        matches = "true"
)
class InvitationDeleteIntegrationTest {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // 스케줄러가 검증 대상 아웃박스를 처리하는 것을 막는다.
    @MockitoBean
    private InvitationPhotoCleanupJob cleanupJob;

    @Value("${INVITATION_DELETE_TEST_OWNER_ID}")
    private Long ownerId;

    @Value("${INVITATION_DELETE_TEST_TEMPLATE_ID}")
    private Long templateId;

    private Long invitationId;
    private String sharedPhotoKey;
    private String publishedOnlyPhotoKey;

    // 새 청첩장에 작성본과 공개본이 각각 참조하는 사진 키를 준비한다.
    @BeforeEach
    void setUp() {
        InvitationCreateRequestDto request =
                BeanUtils.instantiateClass(InvitationCreateRequestDto.class);
        ReflectionTestUtils.setField(request, "templateId", templateId);

        invitationId = invitationService.create(ownerId, request).getId();
        sharedPhotoKey = "invitations/" + invitationId + "/photos/shared.jpeg";
        publishedOnlyPhotoKey =
                "invitations/" + invitationId + "/photos/published-only.jpeg";

        jdbcTemplate.update("""
                UPDATE invitation_documents
                SET body = JSON_OBJECT(
                    'coverPhoto', JSON_OBJECT('storageKey', ?)
                )
                WHERE invitation_id = ? AND kind = 'DRAFT'
                """, sharedPhotoKey, invitationId);

        jdbcTemplate.update("""
                INSERT INTO invitation_documents (
                    invitation_id, template_id, kind, body, created_at, updated_at
                )
                SELECT invitation_id, template_id, 'PUBLISHED',
                       JSON_OBJECT(
                           'coverPhoto', JSON_OBJECT('storageKey', ?),
                           'gallery', JSON_OBJECT(
                               'photos', JSON_ARRAY(JSON_OBJECT('storageKey', ?))
                           )
                       ),
                       CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                FROM invitation_documents
                WHERE invitation_id = ? AND kind = 'DRAFT'
                """, sharedPhotoKey, publishedOnlyPhotoKey, invitationId);

        jdbcTemplate.update("""
                UPDATE invitations
                SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """, invitationId);

        assertThat(findDocumentKinds())
                .containsExactlyInAnyOrder("DRAFT", "PUBLISHED");
    }

    // 자신이 만든 테스트 데이터만 정리한다.
    @AfterEach
    void tearDown() {
        if (invitationId == null) {
            return;
        }

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbcTemplate.update("""
                    DELETE FROM invitation_photo_cleanup
                    WHERE storage_key IN (?, ?)
                    """, sharedPhotoKey, publishedOnlyPhotoKey);
            jdbcTemplate.update("""
                    DELETE FROM invitation_documents WHERE invitation_id = ?
                    """, invitationId);
            jdbcTemplate.update("""
                    DELETE FROM invitations WHERE id = ?
                    """, invitationId);
        });
    }

    // 중복 사진 키를 한 번만 기록하고 청첩장과 두 문서를 삭제한다.
    @Test
    @DisplayName("청첩장 삭제가 커밋되면 두 문서가 사라지고 사진 키가 중복 없이 등록된다")
    void commitsDeletionAndOutboxTogether() {
        invitationService.delete(ownerId, invitationId);

        assertThat(invitationRepository.existsById(invitationId)).isFalse();
        assertThat(findDocumentKinds()).isEmpty();
        assertThat(findOutboxKeys())
                .containsExactlyInAnyOrder(sharedPhotoKey, publishedOnlyPhotoKey);
    }

    // 실제 DELETE SQL 실행 후 예외를 던져 JPA와 JDBC가 함께 롤백되는지 확인한다.
    @Test
    @DisplayName("DELETE SQL 실행 후 롤백하면 청첩장과 문서가 복구되고 아웃박스 등록도 취소된다")
    void rollsBackDeletionAndOutboxTogether() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            invitationService.delete(ownerId, invitationId);
            invitationRepository.flush();

            // 롤백 전에 두 변경이 실제 DB 연결에 반영됐는지 확인한다.
            assertThat(countInvitationRows()).isZero();
            assertThat(findDocumentKinds()).isEmpty();
            assertThat(findOutboxKeys())
                    .containsExactlyInAnyOrder(sharedPhotoKey, publishedOnlyPhotoKey);

            throw new IllegalStateException("삭제 트랜잭션 롤백 검증");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("삭제 트랜잭션 롤백 검증");

        // 트랜잭션 종료 뒤 조회해야 롤백된 DB 상태를 확인할 수 있다.
        assertThat(countInvitationRows()).isEqualTo(1L);
        assertThat(findDocumentKinds())
                .containsExactlyInAnyOrder("DRAFT", "PUBLISHED");
        assertThat(findOutboxKeys()).isEmpty();
    }

    private Long countInvitationRows() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM invitations WHERE id = ?
                """, Long.class, invitationId);
    }

    private List<String> findDocumentKinds() {
        return jdbcTemplate.queryForList("""
                SELECT kind FROM invitation_documents WHERE invitation_id = ?
                """, String.class, invitationId);
    }

    private List<String> findOutboxKeys() {
        return jdbcTemplate.queryForList("""
                SELECT storage_key
                FROM invitation_photo_cleanup
                WHERE storage_key IN (?, ?)
                """, String.class, sharedPhotoKey, publishedOnlyPhotoKey);
    }
}