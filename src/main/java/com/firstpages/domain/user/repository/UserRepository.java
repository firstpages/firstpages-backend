package com.firstpages.domain.user.repository;

import com.firstpages.domain.user.entity.SocialProvider;
import com.firstpages.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 시 기존 사용자 조회
    Optional<User> findByProviderAndProviderId(
            SocialProvider provider,
            String providerId
    );

    // 소셜 계정 존재 여부 확인
    boolean existsByProviderAndProviderId(
            SocialProvider provider,
            String providerId
    );
}
