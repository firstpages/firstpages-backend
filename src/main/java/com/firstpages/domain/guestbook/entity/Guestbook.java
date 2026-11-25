package com.firstpages.domain.guestbook.entity;

import com.firstpages.domain.invitation.entity.Invitation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 하객이 청첩장에 남긴 방명록 메시지
@Entity
@Table(name = "guestbooks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Guestbook {

    // 방명록 메시지 고유 식별자 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 메시지가 등록된 청첩장 (FK)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitation_id", nullable = false)
    private Invitation invitation;

    // 하객이 입력한 작성자 이름
    @Column(name = "author_name", nullable = false, length = 20)
    private String authorName;

    // 하객이 남긴 축하 메시지 본문
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    // 방명록 메시지 작성 일시
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Guestbook(
            Invitation invitation,
            String authorName,
            String message
    ) {
        this.invitation = invitation;
        this.authorName = authorName;
        this.message = message;
    }

    // 방명록 생성 정적 팩토리 메서드
    public static Guestbook create(
            Invitation invitation,
            String authorName,
            String message
    ) {
        return new Guestbook(
                invitation,
                authorName,
                message
        );
    }
}
