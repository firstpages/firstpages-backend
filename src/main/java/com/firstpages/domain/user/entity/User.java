package com.firstpages.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    // 서비스 내부에서 사용하는 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 소셜 로그인 제공자
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    // 소셜 로그인 제공자가 부여한 사용자 식별자
    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    // 서비스에서 사용하는 닉네임
    @Column(name = "nickname", length = 50)
    private String nickname;

    // 생성 시각
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 정보 수정 시각
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private User(
            SocialProvider provider,
            String providerId,
            String nickname
    ) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
    }

    // 기존 사용자 생성 방식과의 호환성 유지
    public static User create(
            SocialProvider provider,
            String providerId
    ) {
        return new User(
                provider,
                providerId,
                null
        );
    }

    // 닉네임을 포함한 사용자 생성
    public static User create(
            SocialProvider provider,
            String providerId,
            String nickname
    ) {
        return new User(
                provider,
                providerId,
                nickname
        );
    }

    // 사용자 닉네임 변경
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

}
