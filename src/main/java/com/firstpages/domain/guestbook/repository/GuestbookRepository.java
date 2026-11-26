package com.firstpages.domain.guestbook.repository;

import com.firstpages.domain.guestbook.entity.Guestbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 방명록 메시지의 데이터 접근을 담당
public interface GuestbookRepository extends JpaRepository<Guestbook, Long> {

    // 청첩장별 방명록을 작성 시각과 ID의 내림차순으로 조회
    Page<Guestbook> findByInvitationIdOrderByCreatedAtDescIdDesc(
            Long invitationId,
            Pageable pageable
    );

    // 청첩장 ID와 메시지 ID가 일치하는 방명록을 삭제하고 삭제 건수를 반환
    // 방어 코드 clearAutomatically(영속성 컨텍스트에서 해당 엔티티 같이 제거)
    @Modifying(clearAutomatically = true)
    @Query("""
            delete from Guestbook guestbook
            where guestbook.invitation.id = :invitationId
                and guestbook.id = :guestbookId
            """)
    int deleteByInvitationIdAndGuestbookId(
            @Param("invitationId") Long invitationId,
            @Param("guestbookId") Long guestbookId
    );
}
