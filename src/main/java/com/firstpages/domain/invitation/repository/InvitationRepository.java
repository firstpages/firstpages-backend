package com.firstpages.domain.invitation.repository;

import com.firstpages.domain.invitation.entity.Invitation;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// 청첩장 관리 정보의 데이터 접근을 담당하는 리포지토리
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    // 사용자가 소유한 청첩장 목록을 페이징하여 조회
    Page<Invitation> findByOwnerId(
            Long ownerId,
            Pageable pageable
    );

    // 외부 공유 식별자로 청첩장 조회
    Optional<Invitation> findByPublicId(
            String publicId
    );

    // 청첩장 ID와 소유자 ID로 청첩장 조회
    Optional<Invitation> findByOwnerIdAndId(
            Long ownerId,
            Long id
    );

    // 특정 청첩장에 대한 사용자 소유권 확인
    boolean existsByIdAndOwnerId(
            Long invitationId,
            Long ownerId
    );

    // 소유자의 청첩장을 조회하면서 낙관적 락과 버전 증가를 적용
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("""
            select invitation
            from Invitation invitation
            where invitation.id = :invitationId
              and invitation.owner.id = :ownerId
            """)
    Optional<Invitation> findByIdAndOwnerIdWithOptimisticLock(
            @Param("invitationId") Long invitationId,
            @Param("ownerId") Long ownerId
    );

    // 소유한 청첩장을 비관적 락을 걸고 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select invitation
        from Invitation invitation
        where invitation.owner.id = :ownerId
          and invitation.id = :invitationId
        """)
    Optional<Invitation> findByOwnerIdAndIdForUpdate(
            @Param("ownerId") Long ownerId,
            @Param("invitationId") Long invitationId
    );
}
