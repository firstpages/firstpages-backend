package com.firstpages.domain.invitation.repository;

import com.firstpages.domain.invitation.entity.Invitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 청첩장 관리 정보의 데이터 접근을 담당하는 리포지토리
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    // 사용자가 소유한 청첩장 목록을 페이징하여 조회
    Page<Invitation> findByOwnerId(Long ownerId, Pageable pageable);

    // 외부 공유 식별자로 청첩장 조회
    Optional<Invitation> findByPublicId(String publicId);

    // 특정 청첩장에 대한 사용자 소유권 확인
    boolean existsByIdAndOwnerId(Long invitationId, Long ownerId);
}
