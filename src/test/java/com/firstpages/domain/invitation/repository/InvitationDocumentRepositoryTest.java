
package com.firstpages.domain.invitation.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import com.firstpages.domain.template.entity.Template;
import com.firstpages.domain.template.repository.TemplateRepository;
import com.firstpages.domain.user.entity.SocialProvider;
import com.firstpages.domain.user.entity.User;
import com.firstpages.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 청첩장 문서의 저장, 조회 및 DB 제약조건 검증
@SpringBootTest
@Transactional
class InvitationDocumentRepositoryTest {

    @Autowired
    private InvitationDocumentRepository documentRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Invitation invitation;
    private Template template;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        User owner = userRepository.save(
                User.create(
                        SocialProvider.KAKAO,
                        UUID.randomUUID().toString()
                )
        );

        // 테스트용 청첩장 생성
        invitation = invitationRepository.save(
                Invitation.create(
                        owner,
                        "테스트 청첩장",
                        UUID.randomUUID().toString().replace("-", "")
                )
        );

        // 테스트용 디자인 템플릿 생성
        template = templateRepository.save(
                Template.create(
                        "test-" + UUID.randomUUID(),
                        "페이퍼 커버",
                        "한 권의 이야기처럼",
                        "테스트용 디자인 템플릿",
                        "minimal",
                        List.of("미니멀", "사진 중심"),
                        "/images/template-samples/camera-moment.png",
                        "gowun-batang",
                        true
                )
        );
    }

    // 테스트용 청첩장 JSON 본문 생성
    private ObjectNode createBody() {
        ObjectNode body = objectMapper.createObjectNode();

        body.put("font", "gowun-batang");
        body.put("groomName", "김지훈");
        body.put("brideName", "이서연");

        return body;
    }

    // 문서 종류에 따른 테스트용 청첩장 문서 생성
    private InvitationDocument createDocument(
            InvitationDocumentKind kind
    ) {
        return InvitationDocument.create(
                invitation,
                kind,
                template,
                createBody()
        );
    }

    @Test
    void 청첩장_문서_저장_및_기본키_자동생성() {
        // given
        InvitationDocument document = createDocument(
                InvitationDocumentKind.DRAFT
        );

        // when
        InvitationDocument saved = documentRepository.save(document);
        entityManager.flush();

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getKind())
                .isEqualTo(InvitationDocumentKind.DRAFT);
    }

    @Test
    void 청첩장과_템플릿_연관관계_조회() {
        // given
        Long documentId = documentRepository.save(
                createDocument(InvitationDocumentKind.DRAFT)
        ).getId();

        entityManager.flush();
        entityManager.clear();

        // when
        InvitationDocument found = documentRepository.findById(documentId)
                .orElseThrow();

        // then
        assertThat(found.getInvitation().getId())
                .isEqualTo(invitation.getId());

        assertThat(found.getTemplate().getId())
                .isEqualTo(template.getId());
    }

    @Test
    void 청첩장_JSON_본문_저장_및_조회() {
        // given
        Long documentId = documentRepository.save(
                createDocument(InvitationDocumentKind.DRAFT)
        ).getId();

        entityManager.flush();
        entityManager.clear();

        // when
        InvitationDocument found = documentRepository.findById(documentId)
                .orElseThrow();

        // then
        assertThat(found.getBody().get("font").asText())
                .isEqualTo("gowun-batang");

        assertThat(found.getBody().get("groomName").asText())
                .isEqualTo("김지훈");

        assertThat(found.getBody().get("brideName").asText())
                .isEqualTo("이서연");
    }

    @Test
    void 하나의_청첩장에_작성본과_공개본_저장() {
        // given
        documentRepository.save(
                createDocument(InvitationDocumentKind.DRAFT)
        );

        documentRepository.save(
                createDocument(InvitationDocumentKind.PUBLISHED)
        );

        entityManager.flush();
        entityManager.clear();

        // when
        List<InvitationDocument> documents =
                documentRepository.findByInvitationId(invitation.getId());

        // then
        assertThat(documents).hasSize(2);

        assertThat(documents)
                .extracting(InvitationDocument::getKind)
                .containsExactlyInAnyOrder(
                        InvitationDocumentKind.DRAFT,
                        InvitationDocumentKind.PUBLISHED
                );
    }

    @Test
    void 동일한_청첩장과_문서종류는_중복_저장할_수_없다() {
        // given
        documentRepository.save(
                createDocument(InvitationDocumentKind.DRAFT)
        );

        entityManager.flush();

        // when & then
        assertThatThrownBy(() -> {
            documentRepository.save(
                    createDocument(InvitationDocumentKind.DRAFT)
            );

            entityManager.flush();
        }).hasRootCauseInstanceOf(
                SQLIntegrityConstraintViolationException.class
        );
    }
}
