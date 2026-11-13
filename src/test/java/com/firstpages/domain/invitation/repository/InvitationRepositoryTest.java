
package com.firstpages.domain.invitation.repository;

import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationStatus;
import com.firstpages.domain.user.entity.SocialProvider;
import com.firstpages.domain.user.entity.User;
import com.firstpages.domain.user.repository.UserRepository;
import com.firstpages.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 청첩장 저장, 조회 및 낙관적 락 동작 검증
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class InvitationRepositoryTest {

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;

    @BeforeEach
    void setUp() {
        // 테스트마다 고유한 소셜 계정 식별자를 사용하여 사용자 생성
        owner = userRepository.save(
                User.create(
                        SocialProvider.KAKAO,
                        UUID.randomUUID().toString()
                )
        );
    }

    // 테스트용 청첩장 생성
    private Invitation createInvitation(String title) {
        return Invitation.create(
                owner,
                title,
                UUID.randomUUID().toString().replace("-", "")
        );
    }

    @Test
    void 청첩장_저장_및_기본키_자동생성() {
        // given
        Invitation invitation = createInvitation("우리의 청첩장");

        // when
        Invitation saved = invitationRepository.save(invitation);
        entityManager.flush();

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(InvitationStatus.DRAFT);
        assertThat(saved.getPublicId()).hasSize(32);
        assertThat(saved.getVersion()).isNotNull();
    }

    @Test
    void 청첩장_소유자_연관관계_조회() {
        // given
        Long invitationId = invitationRepository.save(
                createInvitation("소유자 테스트")
        ).getId();

        entityManager.flush();
        entityManager.clear();

        // when
        Invitation found = invitationRepository.findById(invitationId)
                .orElseThrow();

        // then
        assertThat(found.getOwner().getId()).isEqualTo(owner.getId());
    }

    @Test
    void 공유_식별자로_청첩장_조회() {
        // given
        Invitation saved = invitationRepository.save(
                createInvitation("공유 링크 테스트")
        );

        entityManager.flush();
        entityManager.clear();

        // when
        Invitation found = invitationRepository
                .findByPublicId(saved.getPublicId())
                .orElseThrow();

        // then
        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    @Test
    void 소유자별_청첩장_페이징_조회() {
        // given
        invitationRepository.save(createInvitation("청첩장 A"));
        invitationRepository.save(createInvitation("청첩장 B"));

        entityManager.flush();

        // when
        Page<Invitation> result = invitationRepository.findByOwnerId(
                owner.getId(),
                PageRequest.of(0, 1)
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void 청첩장_소유권_확인() {
        // given
        Long invitationId = invitationRepository.save(
                createInvitation("소유권 테스트")
        ).getId();

        // when
        boolean owned = invitationRepository.existsByIdAndOwnerId(
                invitationId,
                owner.getId()
        );

        // then
        assertThat(owned).isTrue();
    }

    @Test
    void 청첩장_수정시_버전_증가() {
        // given
        Long invitationId = invitationRepository.save(
                createInvitation("수정 전 제목")
        ).getId();

        entityManager.flush();
        entityManager.clear();

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow();

        Integer previousVersion = invitation.getVersion();

        // when
        invitation.updateManagementTitle("수정 후 제목");
        entityManager.flush();
        entityManager.clear();

        // then
        Invitation updated = invitationRepository.findById(invitationId)
                .orElseThrow();

        assertThat(updated.getManagementTitle())
                .isEqualTo("수정 후 제목");

        assertThat(updated.getVersion())
                .isEqualTo(previousVersion + 1);
    }
}
