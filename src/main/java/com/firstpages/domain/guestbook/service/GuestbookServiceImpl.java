package com.firstpages.domain.guestbook.service;

import com.firstpages.domain.guestbook.dto.request.GuestbookCreateRequestDto;
import com.firstpages.domain.guestbook.dto.response.GuestbookItemResponseDto;
import com.firstpages.domain.guestbook.entity.Guestbook;
import com.firstpages.domain.guestbook.exception.GuestbookErrorCode;
import com.firstpages.domain.guestbook.repository.GuestbookRepository;
import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import com.firstpages.domain.invitation.entity.InvitationStatus;
import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.domain.invitation.repository.InvitationDocumentRepository;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

// 공개 청첩장의 방명록 작성 조건을 검증하고 메시지를 저장
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestbookServiceImpl implements GuestbookService {

    // 청첩장 리포지토리
    private final InvitationRepository invitationRepository;

    // 청첩장 문서 리포지토리
    private final InvitationDocumentRepository invitationDocumentRepository;

    // 청첩장 방명록 리포지토리
    private final GuestbookRepository guestbookRepository;

    // 공개 상태와 공개본의 방명록 설정을 확인한 뒤 메시지를 등록
    @Transactional
    @Override
    public void create(
            String publicId,
            GuestbookCreateRequestDto requestDto
    ) {
        // 청첩장 행을 잠근 뒤 현재 공개 상태를 확인하고 청첩장 조회
        Invitation invitation = invitationRepository
                .findByPublicIdForUpdate(publicId)
                .filter(found -> found.getStatus() == InvitationStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND)
                );

        // 현재 하객에게 제공되는 공개본을 조회
        InvitationDocument published = invitationDocumentRepository
                .findByInvitationIdAndKind(invitation.getId(), InvitationDocumentKind.PUBLISHED)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        // 공개본의 방명록 설정 값
        JsonNode enabled = published
                .getBody()
                .path("guestbook")
                .path("enabled");

        // 불리언이 아니거나 false로 설정된 경우
        if (!enabled.isBoolean() || !enabled.booleanValue()) {
            throw new BusinessException(GuestbookErrorCode.GUESTBOOK_DISABLED);
        }

        // 방명록
        Guestbook guestbook = Guestbook.create(
                invitation,
                requestDto.getAuthorName(),
                requestDto.getMessage()
        );

        // 방명록 저장
        guestbookRepository.save(guestbook);
    }

    // 청첩장 소유권을 확인하고 요청한 페이지의 방명록 메시지를 조회
    @Override
    public Page<GuestbookItemResponseDto> getGuestbooks(
            Long userId,
            Long invitationId,
            int page,
            int size
    ) {
        // 페이지는 0부터 시작하며 한 번에 최대 100건까지 조회
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        // 로그인한 사용자가 청첩장을 소유하는지 확인
        boolean exists = invitationRepository.existsByIdAndOwnerId(
                invitationId,
                userId
        );

        // 본인 소유의 청첩장이 아닐 경우
        if (!exists) {
            throw new BusinessException(InvitationErrorCode.INVITATION_NOT_FOUND);
        }

        // 작성 시각과 ID의 내림차순으로 조회하고 각 항목을 응답 DTO로 변환
        return guestbookRepository
                .findByInvitationIdOrderByCreatedAtDescIdDesc(
                        invitationId,
                        PageRequest.of(page, size)
                )
                .map(GuestbookItemResponseDto::from);
    }

    // 청첩장 소유권을 확인하고 해당 청첩장의 방명록 메시지를 삭제
    @Transactional
    @Override
    public void delete(
            Long userId,
            Long invitationId,
            Long guestbookId
    ) {
        // 사용자가 요청한 청첩장을 소유하는지 확인
        boolean exists = invitationRepository.existsByIdAndOwnerId(
                invitationId,
                userId
        );

        // 요청한 청첩장을 소유하지 않을 경우
        if (!exists) {
            throw new BusinessException(InvitationErrorCode.INVITATION_NOT_FOUND);
        }

        // 다른 청첩장의 메시지가 삭제되지 않도록 두 ID로 범위를 제한
        int deletedCount = guestbookRepository.deleteByInvitationIdAndGuestbookId(
                invitationId,
                guestbookId
        );

        // 요청한 청첩장에 해당 방명록이 없어 삭제할 수 없을 경우
        if (deletedCount == 0) {
            throw new BusinessException(GuestbookErrorCode.GUESTBOOK_NOT_FOUND);
        }
    }
}
