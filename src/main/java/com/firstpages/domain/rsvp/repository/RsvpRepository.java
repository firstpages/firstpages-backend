package com.firstpages.domain.rsvp.repository;

import com.firstpages.domain.rsvp.entity.Rsvp;
import com.firstpages.domain.rsvp.entity.RsvpGuestSide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 참석 응답의 조회와 저장을 담당
public interface RsvpRepository extends JpaRepository<Rsvp, Long> {

    // 같은 청첩장에 동일한 이름, 하객 구분, 전화번호 뒷 네 자리로 제출한 응답이 있는지 확인
    boolean existsByInvitationIdAndGuestNameAndGuestSideAndPhoneLastFour(
            Long invitationId,
            String guestName,
            RsvpGuestSide guestSide,
            String phoneLastFour
    );

    // 청첩장의 참석 응답을 제출 시각과 ID의 내림차순으로 조회
    Page<Rsvp> findByInvitationIdOrderByCreatedAtDescIdDesc(
            Long invitationId,
            Pageable pageable
    );

    // 청첩장의 전체 응답을 신랑·신부 측별로 집계
    @Query("""
            select rsvp.guestSide as guestSide,
                   sum(case when rsvp.attending = true
                            then 1L + rsvp.companionCount else 0L end) as attendingGuests,
                   sum(case when rsvp.attending = false
                            then 1L else 0L end) as declinedResponses,
                   sum(case when rsvp.attending = true
                            then rsvp.mealCount else 0 end) as mealGuests
            from Rsvp rsvp
            where rsvp.invitation.id = :invitationId
            group by rsvp.guestSide
            """)
    List<SideSummary> summarizeByGuestSide(
            @Param("invitationId") Long invitationId
    );

    // 신랑 또는 신부 한쪽의 집계 결과
    interface SideSummary {
        // 집계 대상 하객 구분을 반환
        RsvpGuestSide getGuestSide();

        // 본인과 동반인을 포함한 참석 인원을 반환
        long getAttendingGuests();

        // 불참으로 제출한 응답 건수를 반환
        long getDeclinedResponses();

        // 식사할 전체 인원을 반환
        long getMealGuests();
    }
}
