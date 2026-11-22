package com.firstpages.domain.invitation.repository;

import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 청첩장 작성본과 공개본의 데이터 접근을 담당하는 리포지토리
public interface InvitationDocumentRepository extends JpaRepository<InvitationDocument, Long> {

    // 청첩장 ID와 문서 종류로 문서를 조회
    Optional<InvitationDocument> findByInvitationIdAndKind(
            Long invitationId,
            InvitationDocumentKind kind
    );

    // 특정 청첩장에 속한 작성본과 공개본을 조회
    List<InvitationDocument> findByInvitationId(Long invitationId);

    // 로그인 사용자의 청첩장 작성본 목록 및 연관 엔티티 조회
    @Query("""
            select document
            from InvitationDocument document
            join fetch document.invitation invitation
            join fetch document.template template
            where invitation.owner.id = :ownerId
                and document.kind = com.firstpages.domain.invitation.entity.InvitationDocumentKind.DRAFT
            order by document.updatedAt desc, document.id asc
            """)
    List<InvitationDocument> findAllDraftsByOwnerId(
            @Param("ownerId") Long ownerId
    );

    // 청첩장 ID와 소유자 ID가 일치하는 작성본과 연관 엔티티를 조회
    @Query("""
            select document
            from InvitationDocument document
            join fetch document.invitation invitation
            join fetch document.template template
            where invitation.id = :invitationId
                and invitation.owner.id = :ownerId
                and document.kind = com.firstpages.domain.invitation.entity.InvitationDocumentKind.DRAFT
            """)
    Optional<InvitationDocument> findDraftByInvitationIdAndOwnerId(
            @Param("invitationId") Long invitationId,
            @Param("ownerId") Long ownerId
    );

    // 공유 식별자로 공개 상태인 청첩장의 공개본과 템플릿을 조회
    @Query("""
            select document
            from InvitationDocument document
            join fetch document.invitation invitation
            join fetch document.template
            where invitation.publicId = :publicId
              and invitation.status = com.firstpages.domain.invitation.entity.InvitationStatus.PUBLISHED
              and document.kind = com.firstpages.domain.invitation.entity.InvitationDocumentKind.PUBLISHED
            """)
    Optional<InvitationDocument> findPublishedByPublicId(
            @Param("publicId") String publicId
    );
}
