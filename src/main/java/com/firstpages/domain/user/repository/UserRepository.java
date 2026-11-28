package com.firstpages.domain.user.repository;

import com.firstpages.domain.user.entity.SocialProvider;
import com.firstpages.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 내부 회원 ID로 조회하면서 회원 행 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select user
            from User user
            where user.id = :userId
            """)
    Optional<User> findByIdForUpdate(
            @Param("userId") Long userId
    );

    // 소셜 회원번호로 조회하면서 기존 회원 행 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u
            from User u
            where u.provider = :provider
                and u.providerId = :providerId
            """)
    Optional<User> findByProviderAndProviderIdForUpdate(
            @Param("provider") SocialProvider provider,
            @Param("providerId") String providerId
    );

}
