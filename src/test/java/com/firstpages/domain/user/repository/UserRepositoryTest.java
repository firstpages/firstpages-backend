package com.firstpages.domain.user.repository;

import com.firstpages.domain.user.entity.SocialProvider;
import com.firstpages.domain.user.entity.User;
import com.firstpages.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("사용자를 저장하고 ID로 조회할 수 있다")
    void saveAndFindUser() {
        // given
        String providerId = UUID.randomUUID().toString();

        User user = User.create(
                SocialProvider.KAKAO,
                providerId
        );

        // when
        User savedUser = userRepository.saveAndFlush(user);
        Long userId = savedUser.getId();

        entityManager.clear();

        User foundUser = userRepository.findById(userId).orElseThrow();

        // then
        assertThat(userId).isNotNull();
        assertThat(foundUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(foundUser.getProviderId()).isEqualTo(providerId);
        assertThat(foundUser.getNickname()).isNull();
    }

    @Test
    @DisplayName("소셜 로그인 정보로 사용자를 조회할 수 있다")
    void findBySocialAccount() {
        // given
        String providerId = UUID.randomUUID().toString();

        User savedUser = userRepository.saveAndFlush(
                User.create(SocialProvider.KAKAO, providerId)
        );

        entityManager.clear();

        // when
        User foundUser = userRepository.findByProviderAndProviderId(
                        SocialProvider.KAKAO,
                        providerId
                )
                .orElseThrow();

        // then
        assertThat(foundUser.getId())
                .isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("소셜 계정의 존재 여부를 확인할 수 있다")
    void existsBySocialAccount() {
        // given
        String providerId = UUID.randomUUID().toString();

        userRepository.saveAndFlush(
                User.create(SocialProvider.KAKAO, providerId)
        );

        // when
        boolean exists = userRepository.existsByProviderAndProviderId(
                SocialProvider.KAKAO,
                providerId
        );

        boolean notExists = userRepository.existsByProviderAndProviderId(
                SocialProvider.KAKAO,
                "unknown-user"
        );

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("닉네임 변경 시 JPA 변경 감지가 동작한다")
    void updateNickname() {
        // given
        User savedUser = userRepository.saveAndFlush(
                User.create(
                        SocialProvider.KAKAO,
                        UUID.randomUUID().toString()
                )
        );

        Long userId = savedUser.getId();
        entityManager.clear();

        // when
        User user = userRepository.findById(userId).orElseThrow();

        user.updateNickname("첫장사용자");

        entityManager.flush();
        entityManager.clear();

        // then
        User updatedUser = userRepository.findById(userId).orElseThrow();

        assertThat(updatedUser.getNickname()).isEqualTo("첫장사용자");
    }

    @Test
    @DisplayName("사용자 생성 시 Auditing 날짜가 기록된다")
    void auditingCreatedAt() {
        // given
        User user = User.create(
                SocialProvider.KAKAO,
                UUID.randomUUID().toString()
        );

        // when
        userRepository.saveAndFlush(user);

        // then
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자 수정 시 updatedAt이 갱신된다")
    void auditingUpdatedAt() {
        // given
        User savedUser = userRepository.saveAndFlush(
                User.create(
                        SocialProvider.KAKAO,
                        UUID.randomUUID().toString()
                )
        );

        Long userId = savedUser.getId();
        LocalDateTime originalCreatedAt = savedUser.getCreatedAt();
        LocalDateTime originalUpdatedAt = savedUser.getUpdatedAt();

        entityManager.clear();

        // when
        User user = userRepository.findById(userId).orElseThrow();

        user.updateNickname("수정된닉네임");

        entityManager.flush();
        entityManager.clear();

        // then
        User updatedUser = userRepository.findById(userId).orElseThrow();

        assertThat(updatedUser.getCreatedAt()).isEqualTo(originalCreatedAt);

        assertThat(updatedUser.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    @DisplayName("동일한 소셜 계정은 중복 저장할 수 없다")
    void duplicateSocialAccount() {
        // given
        String providerId = UUID.randomUUID().toString();

        userRepository.saveAndFlush(
                User.create(SocialProvider.KAKAO, providerId)
        );

        entityManager.clear();

        // when & then
        assertThatThrownBy(() ->
                userRepository.saveAndFlush(
                        User.create(SocialProvider.KAKAO, providerId)
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}