package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.dto.request.InvitationUpdateRequestDto;
import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.domain.invitation.repository.InvitationDocumentRepository;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import com.firstpages.domain.template.entity.Template;
import com.firstpages.domain.template.repository.TemplateRepository;
import com.firstpages.domain.user.entity.User;
import com.firstpages.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 DB에서 작성본 저장과 낙관적 락 및 롤백 동작을 검증
@SpringBootTest
class InvitationUpdateIntegrationTest {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private InvitationDocumentRepository invitationDocumentRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private InvitationDraftBodyFactory draftBodyFactory;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private Long userId;
    private Long templateId;
    private Long invitationId;
    private Integer initialVersion;

    // 각 테스트가 사용할 독립적인 사용자와 템플릿 및 청첩장을 생성
    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        String identifier = UUID.randomUUID()
                .toString()
                .replace("-", "");

        // 사용자 엔티티의 생성 방식과 무관하게 테스트 사용자만 추가
        jdbcTemplate.update("""
                insert into users (
                    provider,
                    provider_id,
                    nickname,
                    created_at,
                    updated_at
                )
                values (?, ?, ?, current_timestamp(6), current_timestamp(6))
                """,
                "KAKAO",
                "invitation-update-test-" + identifier,
                "저장 테스트 사용자"
        );

        userId = jdbcTemplate.queryForObject("""
                select id
                from users
                where provider = ?
                  and provider_id = ?
                """,
                Long.class,
                "KAKAO",
                "invitation-update-test-" + identifier
        );

        // 테스트 데이터도 별도 트랜잭션에서 커밋하여 다른 스레드에서 조회 가능하게 설정
        transactionTemplate.executeWithoutResult(status -> {
            Template template = templateRepository.save(
                    Template.create(
                            "update-test-" + identifier,
                            "저장 테스트 템플릿",
                            "테스트 부제목",
                            "청첩장 저장 통합 테스트용 템플릿",
                            "minimal",
                            List.of("테스트"),
                            "/images/test-cover.png",
                            "gowun-batang",
                            JsonNodeFactory.instance.objectNode(),
                            true
                    )
            );

            templateId = template.getId();

            User owner = entityManager.getReference(User.class, userId);

            Invitation invitation = invitationRepository.save(
                    Invitation.create(
                            owner,
                            "",
                            UUID.randomUUID()
                                    .toString()
                                    .replace("-", "")
                    )
            );

            invitationId = invitation.getId();

            invitationDocumentRepository.save(
                    InvitationDocument.create(
                            invitation,
                            InvitationDocumentKind.DRAFT,
                            template,
                            draftBodyFactory.create(template)
                    )
            );
        });

        initialVersion = readSnapshot().version;
    }

    // 커밋된 테스트 데이터만 외래 키 순서에 맞춰 제거
    @AfterEach
    void tearDown() {
        if (invitationId != null) {
            jdbcTemplate.update(
                    "delete from invitation_documents where invitation_id = ?",
                    invitationId
            );

            jdbcTemplate.update(
                    "delete from invitations where id = ?",
                    invitationId
            );
        }

        if (templateId != null) {
            jdbcTemplate.update(
                    "delete from templates where id = ?",
                    templateId
            );
        }

        if (userId != null) {
            jdbcTemplate.update(
                    "delete from users where id = ?",
                    userId
            );
        }
    }

    // 제목과 작성본 변경이 실제로 커밋되는지 검증
    @Test
    @DisplayName("청첩장을 저장하면 제목과 작성본이 DB에 반영된다")
    void updatePersistsTitleAndBody() {
        // given
        ObjectNode body = createBody("박서준");

        InvitationUpdateRequestDto requestDto = createRequest(
                "서준과 다은의 결혼식",
                body,
                initialVersion
        );

        // when
        invitationService.update(userId, invitationId, requestDto);

        // then
        InvitationSnapshot snapshot = readSnapshot();

        assertThat(snapshot.managementTitle)
                .isEqualTo("서준과 다은의 결혼식");

        assertThat(snapshot.body).isEqualTo(body);

        assertThat(snapshot.version)
                .isGreaterThan(initialVersion);
    }

    // 부모 제목이 변경되지 않아도 작성본 저장으로 부모 버전이 증가하는지 검증
    @Test
    @DisplayName("관리용 제목이 같아도 작성본을 수정하면 청첩장 버전이 증가한다")
    void updateOnlyBodyIncrementsParentVersion() {
        // given
        ObjectNode body = createBody("본문만 수정한 신랑");

        InvitationUpdateRequestDto requestDto = createRequest(
                "",
                body,
                initialVersion
        );

        // when
        invitationService.update(userId, invitationId, requestDto);

        // then
        InvitationSnapshot snapshot = readSnapshot();

        assertThat(snapshot.managementTitle).isEmpty();
        assertThat(snapshot.body).isEqualTo(body);

        assertThat(snapshot.version)
                .isGreaterThan(initialVersion);
    }

    // 이전 버전으로 저장을 요청하면 먼저 저장된 데이터가 유지되는지 검증
    @Test
    @DisplayName("이전 버전으로 저장하면 충돌 예외가 발생하고 기존 데이터가 유지된다")
    void updateWithStaleVersionDoesNotChangeData() {
        // given
        ObjectNode firstBody = createBody("먼저 저장한 신랑");

        invitationService.update(
                userId,
                invitationId,
                createRequest(
                        "먼저 저장한 제목",
                        firstBody,
                        initialVersion
                )
        );

        InvitationSnapshot before = readSnapshot();

        InvitationUpdateRequestDto staleRequest = createRequest(
                "덮어쓰려는 제목",
                createBody("나중 요청의 신랑"),
                initialVersion
        );

        // when & then
        assertThatThrownBy(() ->
                invitationService.update(
                        userId,
                        invitationId,
                        staleRequest
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                InvitationErrorCode.INVITATION_VERSION_CONFLICT
                        )
        );

        InvitationSnapshot after = readSnapshot();

        assertThat(after.managementTitle)
                .isEqualTo(before.managementTitle);

        assertThat(after.body).isEqualTo(before.body);
        assertThat(after.version).isEqualTo(before.version);
    }

    // 두 트랜잭션이 같은 버전을 조회한 뒤 저장하면 하나만 커밋되는지 검증
    @Test
    @DisplayName("같은 버전으로 동시에 작성본을 저장하면 하나만 성공하고 나머지는 롤백된다")
    void concurrentUpdatesAllowOnlyOneCommit() throws Exception {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch bothLoaded = new CountDownLatch(2);

        ObjectNode firstBody = createBody("첫 번째 요청");
        ObjectNode secondBody = createBody("두 번째 요청");

        try {
            // when
            Future<Boolean> firstResult = executor.submit(() ->
                    saveConcurrently(firstBody, bothLoaded)
            );

            Future<Boolean> secondResult = executor.submit(() ->
                    saveConcurrently(secondBody, bothLoaded)
            );

            boolean firstSucceeded = firstResult.get(30, TimeUnit.SECONDS);
            boolean secondSucceeded = secondResult.get(30, TimeUnit.SECONDS);

            // then
            assertThat(List.of(firstSucceeded, secondSucceeded))
                    .containsExactlyInAnyOrder(true, false);

            InvitationSnapshot snapshot = readSnapshot();

            JsonNode successfulBody = firstSucceeded
                    ? firstBody
                    : secondBody;

            // 실패한 트랜잭션의 본문 변경이 남지 않았는지 검증
            assertThat(snapshot.body).isEqualTo(successfulBody);

            assertThat(snapshot.version)
                    .isGreaterThan(initialVersion);
        } finally {
            executor.shutdownNow();

            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "동시 저장 테스트 스레드가 종료되지 않았습니다."
                );
            }
        }
    }

    // 각 트랜잭션이 같은 버전을 조회하도록 동기화한 뒤 작성본을 변경
    private boolean saveConcurrently(
            JsonNode body,
            CountDownLatch bothLoaded
    ) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Invitation invitation = invitationRepository
                        .findByIdAndOwnerIdWithOptimisticLock(
                                invitationId,
                                userId
                        )
                        .orElseThrow();

                // 두 트랜잭션 모두 처음 조회한 버전으로 작업하는지 검증
                assertThat(invitation.getVersion())
                        .isEqualTo(initialVersion);

                InvitationDocument document =
                        invitationDocumentRepository
                                .findByInvitationIdAndKind(
                                        invitationId,
                                        InvitationDocumentKind.DRAFT
                                )
                                .orElseThrow();

                bothLoaded.countDown();
                awaitBothTransactions(bothLoaded);

                // 부모는 변경하지 않고 자식 본문만 변경하여 강제 버전 증가를 검증
                document.updateBody(body);
            });

            return true;
        } catch (RuntimeException exception) {
            if (isOptimisticLockConflict(exception)) {
                return false;
            }

            throw exception;
        }
    }

    // 두 트랜잭션이 모두 조회를 완료할 때까지 제한 시간 내에서 대기
    private void awaitBothTransactions(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "두 트랜잭션의 조회 완료를 기다리다 시간이 초과되었습니다."
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "동시 저장 테스트 대기가 중단되었습니다.",
                    exception
            );
        }
    }

    // 커밋 예외가 래핑된 경우에도 낙관적 락 충돌인지 확인
    private boolean isOptimisticLockConflict(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof OptimisticLockException
                    || current instanceof OptimisticLockingFailureException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    // 별도 트랜잭션에서 DB에 확정된 제목과 버전 및 작성본을 조회
    private InvitationSnapshot readSnapshot() {
        return transactionTemplate.execute(status -> {
            Invitation invitation = invitationRepository
                    .findByOwnerIdAndId(userId, invitationId)
                    .orElseThrow();

            InvitationDocument document = invitationDocumentRepository
                    .findByInvitationIdAndKind(
                            invitationId,
                            InvitationDocumentKind.DRAFT
                    )
                    .orElseThrow();

            return new InvitationSnapshot(
                    invitation.getManagementTitle(),
                    invitation.getVersion(),
                    document.getBody().deepCopy()
            );
        });
    }

    // 보호된 기본 생성자를 가진 요청 DTO에 테스트 값을 설정
    private InvitationUpdateRequestDto createRequest(
            String managementTitle,
            JsonNode body,
            Integer version
    ) {
        InvitationUpdateRequestDto requestDto =
                BeanUtils.instantiateClass(
                        InvitationUpdateRequestDto.class
                );

        ReflectionTestUtils.setField(
                requestDto,
                "managementTitle",
                managementTitle
        );
        ReflectionTestUtils.setField(requestDto, "body", body);
        ReflectionTestUtils.setField(requestDto, "version", version);

        return requestDto;
    }

    // 실제 초기 작성본 구조를 유지하면서 테스트할 신랑 이름만 변경
    private ObjectNode createBody(String groomName) {
        return transactionTemplate.execute(status -> {
            InvitationDocument document = invitationDocumentRepository
                    .findByInvitationIdAndKind(
                            invitationId,
                            InvitationDocumentKind.DRAFT
                    )
                    .orElseThrow();

            ObjectNode body = (ObjectNode) document.getBody().deepCopy();

            ObjectNode basic = (ObjectNode) body.path("basic");
            basic.put("groomName", groomName);

            return body;
        });
    }

    // 트랜잭션 종료 후에도 비교할 수 있도록 조회 결과를 보관
    private static class InvitationSnapshot {

        private final String managementTitle;
        private final Integer version;
        private final JsonNode body;

        private InvitationSnapshot(
                String managementTitle,
                Integer version,
                JsonNode body
        ) {
            this.managementTitle = managementTitle;
            this.version = version;
            this.body = body;
        }
    }
}