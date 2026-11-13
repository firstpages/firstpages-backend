package com.firstpages.domain.invitation.entity;

import com.firstpages.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@Table(name = "invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Invitation {

    // 청첩장 고유 식별자 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 청첩장 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // 사용자가 청첩장을 구분하기 위한 관리용 제목
    @Column(name = "management_title", length = 50, nullable = false)
    private String managementTitle = "";

    // 외부 공유 링크에 사용하는 고유 식별자
    @Column(name = "public_id", length = 32, nullable = false, unique = true)
    private String publicId;

    // 청첩장 공개 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private InvitationStatus status = InvitationStatus.DRAFT;

    // 낙관적 락을 위한 버전
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    // 청첩장 생성 일시
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 청첩장 최종 수정 일시
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 생성자
    private Invitation(
            User owner,
            String managementTitle,
            String publicId
    ) {
        this.owner = owner;
        this.managementTitle = managementTitle;
        this.publicId = publicId;
    }

    // 새로운 청첩장 생성 정적 팩토리 메서드
    public static Invitation create(
            User owner,
            String managementTitle,
            String publicId
    ) {
        return new Invitation(
                owner,
                managementTitle,
                publicId
        );
    }

    // 청첩장 관리용 제목 변경
    public void updateManagementTitle(String newManagementTitle) {
        this.managementTitle = newManagementTitle;
    }

}
