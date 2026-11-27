package com.firstpages.domain.rsvp.entity;

import com.firstpages.domain.invitation.entity.Invitation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "rsvp_responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Rsvp {

    // 참석 응답의 고유 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 응답을 받은 청첩장
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitation_id", nullable = false)
    private Invitation invitation;

    // 하객이 입력한 이름
    @Column(name = "guest_name", nullable = false, length = 20)
    private String guestName;

    // 신랑 측 또는 신부 측 하객 구분
    @Enumerated(EnumType.STRING)
    @Column(name = "guest_side", nullable = false, length = 16)
    private RsvpGuestSide guestSide;

    // 중복 응답 판정에 사용하는 휴대전화 번호 뒷 4자리
    @Column(name = "phone_last_four", nullable = false, length = 4)
    private String phoneLastFour;

    // 하객 본인의 참석 여부
    @Column(name = "attending", nullable = false)
    private boolean attending;

    // 하객 본인을 제외한 동반 참석 인원
    @Column(name = "companion_count", nullable = false)
    private int companionCount;

    // 하객 본인과 동반인 중 식사할 인원
    @Column(name = "meal_count", nullable = false)
    private int mealCount;

    // 하객이 참석 응답을 제출한 시각
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 검증된 하객 정보와 참석 인원으로 응답을 생성
    private Rsvp(
            Invitation invitation,
            String guestName,
            RsvpGuestSide guestSide,
            String phoneLastFour,
            boolean attending,
            int companionCount,
            int mealCount
    ) {
        this.invitation = invitation;
        this.guestName = guestName;
        this.guestSide = guestSide;
        this.phoneLastFour = phoneLastFour;
        this.attending = attending;
        this.companionCount = companionCount;
        this.mealCount = mealCount;
    }

    // 제출 조건을 검증한 서비스에서 저장할 RSVP 생성
    public static Rsvp create(
            Invitation invitation,
            String guestName,
            RsvpGuestSide guestSide,
            String phoneLastFour,
            boolean attending,
            int companionCount,
            int mealCount
    ) {
        return new Rsvp(
                invitation,
                guestName,
                guestSide,
                phoneLastFour,
                attending,
                companionCount,
                mealCount
        );
    }
}
