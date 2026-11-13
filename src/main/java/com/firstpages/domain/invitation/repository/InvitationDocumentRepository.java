package com.firstpages.domain.invitation.repository;

import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 청첩장 작성본과 공개본의 데이터 접근을 담당하는 리포지토리
public interface InvitationDocumentRepository extends JpaRepository<InvitationDocument, Long> {

    // 청첩장 ID와 문서 종류로 청첩장 문서 조회
    Optional<InvitationDocument> findByInvitationIdAndKind(
            Long invitationId,
            InvitationDocumentKind kind
    );

    // 특정 청첩장의 모든 문서 조회 (페이징 없음)
    List<InvitationDocument> findByInvitationId(Long invitationId);

    // 특정 청첩장의 모든 문서 페이징 조회
    Page<InvitationDocument> findByInvitationId(
            Long invitationId,
            Pageable pageable
    );

    // 문서 종류별 전체 목록 조회 (페이징 없음)
    List<InvitationDocument> findByKind(InvitationDocumentKind kind);

    // 문서 종류별 전체 목록 페이징 조회
    Page<InvitationDocument> findByKind(
            InvitationDocumentKind kind,
            Pageable pageable
    );
}
