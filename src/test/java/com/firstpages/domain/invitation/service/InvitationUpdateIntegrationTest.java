package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.dto.request.InvitationPublishRequestDto;
import com.firstpages.domain.invitation.dto.request.InvitationUpdateRequestDto;
import com.firstpages.domain.invitation.dto.response.InvitationPublishResponseDto;
import com.firstpages.domain.invitation.dto.response.PublicInvitationResponseDto;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 DB에서 작성본 저장, 버전 충돌, 하객용 공개 조회를 검증
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

    // 각 테스트에서 사용할 사용자, 템플릿, 청첩장과 작성본을 생성
    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        String identifier = UUID.randomUUID().toString().replace("-", "");
        String providerId = "invitation-update-test-" + identifier;

        jdbcTemplate.update("""
                insert into users (
                    provider,
                    provider_id,
                    nickname,
                    created_at,
                    updated_at
                )
                values (?, ?, ?, current_timestamp(6), current_timestamp(6))
                """, "KAKAO", providerId, "저장 테스트 사용자");

        userId = jdbcTemplate.queryForObject("""
                select id
                from users
                where provider = ?
                  and provider_id = ?
                """, Long.class, "KAKAO", providerId);

        // 다른 트랜잭션과 테스트 스레드에서 읽을 수 있도록 초기 데이터를 커밋
        transactionTemplate.executeWithoutResult(status -> {
            Template template = templateRepository.save(Template.create(
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
            ));
            templateId = template.getId();

            User owner = entityManager.getReference(User.class, userId);

            Invitation invitation = invitationRepository.save(Invitation.create(
                    owner,
                    "",
                    UUID.randomUUID().toString().replace("-", "")
            ));
            invitationId = invitation.getId();

            invitationDocumentRepository.save(InvitationDocument.create(
                    invitation,
                    InvitationDocumentKind.DRAFT,
                    template,
                    draftBodyFactory.create(template)
            ));
        });

        initialVersion = readSnapshot().version;
    }

    // 테스트에서 커밋한 데이터만 외래 키 순서에 맞춰 삭제
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
            jdbcTemplate.update("delete from templates where id = ?", templateId);
        }

        if (userId != null) {
            jdbcTemplate.update("delete from users where id = ?", userId);
        }
    }

    // 제목과 작성본 변경이 실제로 커밋되는지 검증
    @Test
    @DisplayName("청첩장을 저장하면 제목과 작성본이 DB에 반영된다")
    void updatePersistsTitleAndBody() {
        ObjectNode body = createBody("박서준");
        InvitationUpdateRequestDto request = createRequest(
                "서준과 다은의 결혼식", body, initialVersion
        );

        invitationService.update(userId, invitationId, request);

        InvitationSnapshot snapshot = readSnapshot();

        assertThat(snapshot.managementTitle).isEqualTo("서준과 다은의 결혼식");
        assertThat(snapshot.body).isEqualTo(body);
        assertThat(snapshot.version).isGreaterThan(initialVersion);
    }

    // 공개본이 있어도 부모 청첩장이 미공개면 하객이 조회할 수 없는지 검증
    @Test
    @DisplayName("공개본이 있어도 청첩장이 미공개 상태면 하객이 조회할 수 없다")
    void getPublicInvitationRejectsDraftStatus() {
        String publicId = invitationRepository.findById(invitationId)
                .orElseThrow()
                .getPublicId();

        // 부모의 공개 상태 조건을 검증하기 위해 공개본 문서만 생성
        transactionTemplate.executeWithoutResult(status -> {
            InvitationDocument draft = findDocument(InvitationDocumentKind.DRAFT)
                    .orElseThrow();

            invitationDocumentRepository.save(InvitationDocument.create(
                    draft.getInvitation(),
                    InvitationDocumentKind.PUBLISHED,
                    draft.getTemplate(),
                    draft.getBody()
            ));
        });

        assertThatThrownBy(() -> invitationService.getPublicInvitation(publicId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND)
                );
    }

    // 작성본을 다시 저장해도 하객에게는 마지막 공개본이 반환되는지 검증
    @Test
    @DisplayName("공개 후 작성본만 수정하면 하객은 마지막 공개본을 조회한다")
    void getPublicInvitationReturnsLastPublishedBody() {
        invitationService.update(
                userId,
                invitationId,
                createRequest("", createBody("공개한 이름"), initialVersion)
        );

        Integer versionAfterSave =
                invitationService.getUpdateResult(userId, invitationId).getVersion();

        invitationService.publish(
                userId, invitationId, createPublishRequest(versionAfterSave)
        );

        InvitationPublishResponseDto published =
                invitationService.getPublishResult(userId, invitationId);

        invitationService.update(
                userId,
                invitationId,
                createRequest("", createBody("아직 공개하지 않은 이름"), published.getVersion())
        );

        PublicInvitationResponseDto response =
                invitationService.getPublicInvitation(published.getPublicId());

        assertThat(response.getPublicId()).isEqualTo(published.getPublicId());
        assertThat(response.getBody().path("basic").path("groomName").stringValue())
                .isEqualTo("공개한 이름");
    }

    // 제목을 유지하고 작성본만 변경해도 부모 버전이 증가하는지 검증
    @Test
    @DisplayName("관리용 제목이 같아도 작성본을 수정하면 청첩장 버전이 증가한다")
    void updateOnlyBodyIncrementsParentVersion() {
        ObjectNode body = createBody("본문만 수정한 신랑");

        invitationService.update(
                userId, invitationId, createRequest("", body, initialVersion)
        );

        InvitationSnapshot snapshot = readSnapshot();

        assertThat(snapshot.managementTitle).isEmpty();
        assertThat(snapshot.body).isEqualTo(body);
        assertThat(snapshot.version).isGreaterThan(initialVersion);
    }

    // 이전 버전으로 저장하면 먼저 저장된 데이터가 유지되는지 검증
    @Test
    @DisplayName("이전 버전으로 저장하면 충돌 예외가 발생하고 기존 데이터가 유지된다")
    void updateWithStaleVersionDoesNotChangeData() {
        ObjectNode firstBody = createBody("먼저 저장한 신랑");

        invitationService.update(
                userId,
                invitationId,
                createRequest("먼저 저장한 제목", firstBody, initialVersion)
        );

        InvitationSnapshot before = readSnapshot();
        InvitationUpdateRequestDto staleRequest = createRequest(
                "덮어쓰려는 제목",
                createBody("나중 요청의 신랑"),
                initialVersion
        );

        assertThatThrownBy(() -> invitationService.update(userId, invitationId, staleRequest))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(InvitationErrorCode.INVITATION_VERSION_CONFLICT)
                );

        InvitationSnapshot after = readSnapshot();

        assertThat(after.managementTitle).isEqualTo(before.managementTitle);
        assertThat(after.body).isEqualTo(before.body);
        assertThat(after.version).isEqualTo(before.version);
    }

    // 두 트랜잭션이 같은 버전으로 저장하면 한 건만 커밋되는지 검증
    @Test
    @DisplayName("같은 버전으로 동시에 작성본을 저장하면 하나만 성공하고 나머지는 롤백된다")
    void concurrentUpdatesAllowOnlyOneCommit() throws Exception {
        ObjectNode firstBody = createBody("첫 번째 요청");
        ObjectNode secondBody = createBody("두 번째 요청");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch bothLoaded = new CountDownLatch(2);

        try {
            Future<Boolean> firstResult =
                    executor.submit(() -> saveConcurrently(firstBody, bothLoaded));
            Future<Boolean> secondResult =
                    executor.submit(() -> saveConcurrently(secondBody, bothLoaded));

            boolean firstSucceeded = firstResult.get(30, TimeUnit.SECONDS);
            boolean secondSucceeded = secondResult.get(30, TimeUnit.SECONDS);

            assertThat(List.of(firstSucceeded, secondSucceeded))
                    .containsExactlyInAnyOrder(true, false);

            InvitationSnapshot snapshot = readSnapshot();
            JsonNode successfulBody = firstSucceeded ? firstBody : secondBody;

            assertThat(snapshot.body).isEqualTo(successfulBody);
            assertThat(snapshot.version).isGreaterThan(initialVersion);
        } finally {
            executor.shutdownNow();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 저장 테스트 스레드가 종료되지 않았습니다.");
            }
        }
    }

    // 두 트랜잭션에서 같은 버전을 읽은 뒤 각각 작성본을 변경
    private boolean saveConcurrently(JsonNode body, CountDownLatch bothLoaded) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Invitation invitation = invitationRepository
                        .findByIdAndOwnerIdWithOptimisticLock(invitationId, userId)
                        .orElseThrow();

                assertThat(invitation.getVersion()).isEqualTo(initialVersion);

                InvitationDocument draft = findDocument(InvitationDocumentKind.DRAFT)
                        .orElseThrow();

                bothLoaded.countDown();
                awaitBothTransactions(bothLoaded);

                draft.updateBody(body);
            });

            return true;
        } catch (RuntimeException exception) {
            if (isOptimisticLockConflict(exception)) {
                return false;
            }
            throw exception;
        }
    }

    // 두 트랜잭션의 조회 완료를 제한 시간 동안 대기
    private void awaitBothTransactions(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("두 트랜잭션의 조회 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 저장 테스트 대기가 중단되었습니다.", exception);
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

    // 테스트 청첩장에서 지정한 종류의 문서를 조회
    private Optional<InvitationDocument> findDocument(InvitationDocumentKind kind) {
        return invitationDocumentRepository.findByInvitationIdAndKind(invitationId, kind);
    }

    // 별도 트랜잭션에서 DB에 저장된 제목, 버전, 작성본을 조회
    private InvitationSnapshot readSnapshot() {
        return transactionTemplate.execute(status -> {
            Invitation invitation = invitationRepository
                    .findByOwnerIdAndId(userId, invitationId)
                    .orElseThrow();

            InvitationDocument draft = findDocument(InvitationDocumentKind.DRAFT)
                    .orElseThrow();

            return new InvitationSnapshot(
                    invitation.getManagementTitle(),
                    invitation.getVersion(),
                    draft.getBody().deepCopy()
            );
        });
    }

    // 저장 요청 DTO에 제목, 작성본, 버전을 설정
    private InvitationUpdateRequestDto createRequest(
            String managementTitle,
            JsonNode body,
            Integer version
    ) {
        InvitationUpdateRequestDto request =
                BeanUtils.instantiateClass(InvitationUpdateRequestDto.class);

        ReflectionTestUtils.setField(request, "managementTitle", managementTitle);
        ReflectionTestUtils.setField(request, "body", body);
        ReflectionTestUtils.setField(request, "version", version);
        return request;
    }

    // 공개 요청 DTO에 테스트할 버전을 설정
    private InvitationPublishRequestDto createPublishRequest(Integer version) {
        InvitationPublishRequestDto request =
                BeanUtils.instantiateClass(InvitationPublishRequestDto.class);

        ReflectionTestUtils.setField(request, "version", version);
        return request;
    }

    // 저장된 작성본을 복사하고 테스트할 신랑 이름을 설정
    private ObjectNode createBody(String groomName) {
        return transactionTemplate.execute(status -> {
            InvitationDocument draft = findDocument(InvitationDocumentKind.DRAFT)
                    .orElseThrow();

            ObjectNode body = (ObjectNode) draft.getBody().deepCopy();
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

        private InvitationSnapshot(String managementTitle, Integer version, JsonNode body) {
            this.managementTitle = managementTitle;
            this.version = version;
            this.body = body;
        }
    }
}