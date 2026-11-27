package com.firstpages.domain.rsvp.service;

import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import com.firstpages.domain.invitation.entity.InvitationStatus;
import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.domain.invitation.repository.InvitationDocumentRepository;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import com.firstpages.domain.rsvp.dto.request.RsvpCreateRequestDto;
import com.firstpages.domain.rsvp.dto.request.RsvpUpdateRequestDto;
import com.firstpages.domain.rsvp.dto.response.RsvpItemResponseDto;
import com.firstpages.domain.rsvp.dto.response.RsvpListResponseDto;
import com.firstpages.domain.rsvp.entity.Rsvp;
import com.firstpages.domain.rsvp.entity.RsvpGuestSide;
import com.firstpages.domain.rsvp.exception.RsvpErrorCode;
import com.firstpages.domain.rsvp.repository.RsvpRepository;
import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.response.PageResponse;
import com.firstpages.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.EnumMap;
import java.util.Map;

// 공개 청첩장의 참석 응답 조건을 검증하고 응답을 저장
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RsvpServiceImpl implements RsvpService {

    // 청첩장 리포지토리
    private final InvitationRepository invitationRepository;

    // 청첩장 문서 리포지토리
    private final InvitationDocumentRepository invitationDocumentRepository;

    // 하객 참석 리포지토리
    private final RsvpRepository rsvpRepository;

    // 공개 여부, 참석 설정, 인원과 중복 제출을 검사한 뒤 응답을 저장
    @Transactional
    @Override
    public void create(
            String publicId,
            RsvpCreateRequestDto requestDto
    ) {
        // 청첩장 행을 잠근 뒤 현재 공개 상태를 확인
        Invitation invitation = invitationRepository
                .findByPublicIdForUpdate(publicId)
                .filter(found -> found.getStatus() == InvitationStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        // 하객에게 적용되는 현재 공개본을 조회
        InvitationDocument published = invitationDocumentRepository
                .findByInvitationIdAndKind(
                        invitation.getId(),
                        InvitationDocumentKind.PUBLISHED
                )
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        // 공개본의 참석 응답 활성화 값을 조회
        JsonNode enabled = published
                .getBody()
                .path("rsvp")
                .path("enabled");

        // 참석 응답 설정이 JSON 불리언 true일 때만 작성 가능
        if (!enabled.isBoolean() || !enabled.booleanValue()) {
            throw new BusinessException(RsvpErrorCode.RSVP_DISABLED);
        }

        // 동반 인원 수
        Integer companionCount = requestDto.getCompanionCount();

        // 식사 인원 수
        Integer mealCount = requestDto.getMealCount();

        // 참석 여부
        boolean attending = requestDto.getAttending();

        // 참석 측
        RsvpGuestSide guestSide = requestDto.getGuestSide();

        // 휴대전화 마지막 4자리
        String phoneLastFour = requestDto.getPhoneLastFour();

        // 참석 여부에 따라 동반 인원과 식사 인원의 조합을 검증
        validateCounts(
                companionCount,
                mealCount,
                attending
        );

        // 하객의 이름(앞뒤 공백 제거)
        String guestName = requestDto
                .getGuestName()
                .strip();

        // 같은 청첩장에 이름, 하객 구분, 전화번호 뒷 네 자리가 일치하는 응답 조회
        boolean alreadySubmitted = rsvpRepository.existsByInvitationIdAndGuestNameAndGuestSideAndPhoneLastFour(
                invitation.getId(),
                guestName,
                guestSide,
                phoneLastFour
        );

        // 기존 응답이 있으면 그대로 보존하고 새 제출을 거절
        if (alreadySubmitted) {
            throw new BusinessException(RsvpErrorCode.RSVP_ALREADY_SUBMITTED);
        }

        // 불참 응답의 동반 인원과 식사 인원은 0명으로 정규화해 엔티티를 생성
        Rsvp rsvp = Rsvp.create(
                invitation,
                guestName,
                guestSide,
                phoneLastFour,
                attending,
                attending ? companionCount : 0,
                attending ? mealCount : 0
        );

        // 검증을 마친 응답을 해당 청첩장에 연결해 저장
        rsvpRepository.save(rsvp);
    }

    // 소유권을 확인하고 참석 응답 페이지와 전체 인원 집계를 반환
    @Override
    public RsvpListResponseDto getRsvps(
            Long userId,
            Long invitationId,
            int page,
            int size
    ) {
        // 페이지 범위를 적용
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        // 로그인한 사용자가 해당 청첩장을 소유하는지 확인
        if (!invitationRepository.existsByIdAndOwnerId(invitationId, userId)) {
            throw new BusinessException(InvitationErrorCode.INVITATION_NOT_FOUND);
        }

        Page<RsvpItemResponseDto> mapped = rsvpRepository
                .findByInvitationIdOrderByCreatedAtDescIdDesc(
                        invitationId,
                        PageRequest.of(page, size)
                )
                .map(RsvpItemResponseDto::from);

        PageResponse<RsvpItemResponseDto> pageResponse = PageResponse.from(mapped);

        Map<RsvpGuestSide, RsvpListResponseDto.SideTotals> totals = new EnumMap<>(RsvpGuestSide.class);

        for (RsvpRepository.SideSummary row : rsvpRepository.summarizeByGuestSide(invitationId)) {
            totals.put(
                    row.getGuestSide(),
                    RsvpListResponseDto.SideTotals.of(
                            row.getAttendingGuests(),
                            row.getDeclinedResponses(),
                            row.getMealGuests()
                    )
            );
        }

        var empty = RsvpListResponseDto.SideTotals.of(
                0,
                0,
                0
        );

        var summary = RsvpListResponseDto.Summary.of(
                totals.getOrDefault(RsvpGuestSide.GROOM, empty),
                totals.getOrDefault(RsvpGuestSide.BRIDE, empty)
        );

        return RsvpListResponseDto.of(
                pageResponse,
                summary
        );
    }

    // 소유권과 수정 내용을 검증한 뒤 변경된 RSVP 응답을 반환
    @Transactional
    @Override
    public RsvpItemResponseDto update(
            Long userId,
            Long invitationId,
            Long rsvpId,
            RsvpUpdateRequestDto requestDto
    ) {
        // 소유권을 확인하고 같은 청첩장의 하객 제출과 편집 순서를 보장
        invitationRepository
                .findByOwnerIdAndIdForUpdate(userId, invitationId)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        // 요청한 청첩장에 속한 응답을 조회하고 편집할 행을 잠그기
        Rsvp rsvp = rsvpRepository
                .findByInvitationIdAndRsvpIdForUpdate(invitationId, rsvpId)
                .orElseThrow(() -> new BusinessException(
                        RsvpErrorCode.RSVP_NOT_FOUND
                ));

        // 하객 이름
        String guestName = requestDto.getGuestName().strip();

        // 하객 구분
        RsvpGuestSide guestSide = requestDto.getGuestSide();

        // 참석 여부
        boolean attending = requestDto.getAttending();

        // 동반 인원
        int companionCount = requestDto.getCompanionCount();

        // 식사 인원
        int mealCount = requestDto.getMealCount();

        // 참석 여부에 따라 허용되는 동반 인원과 식사 인원의 조합을 검증
        validateCounts(
                companionCount,
                mealCount,
                attending
        );

        // 현재 응답을 제외하고 수정할 이름, 하객 측, 기존 전화번호 뒷 네 자리로 중복을 확인
        boolean alreadySubmitted = rsvpRepository.existsByInvitationIdAndGuestNameAndGuestSideAndPhoneLastFourAndIdNot(
                invitationId,
                guestName,
                guestSide,
                rsvp.getPhoneLastFour(),
                rsvpId
        );

        // 다른 응답과 중복되면 편집을 거절
        if (alreadySubmitted) {
            throw new BusinessException(RsvpErrorCode.RSVP_ALREADY_SUBMITTED);
        }

        rsvp.update(
                guestName,
                guestSide,
                attending,
                companionCount,
                mealCount
        );

        // 편집 결과를 목록 항목과 같은 형식으로 반환
        return RsvpItemResponseDto.from(rsvp);
    }

    // 소유권을 확인한 뒤 요청한 청첩장의 참석 응답 한 건을 삭제
    @Transactional
    @Override
    public void delete(
            Long userId,
            Long invitationId,
            Long rsvpId
    ) {

        // 요청자가 청첩장을 소유하는지 확인
        if (!invitationRepository.existsByIdAndOwnerId(
                invitationId,
                userId
        )) {
            throw new BusinessException(InvitationErrorCode.INVITATION_NOT_FOUND);
        }

        // 두 식별자가 일치하는 응답의 삭제 건수를 확인
        int deleted = rsvpRepository.deleteByInvitationIdAndRsvpId(
                invitationId,
                rsvpId
        );

        // 삭제할 rsvp가 없을 경우
        if (deleted != 1) {
            throw new BusinessException(RsvpErrorCode.RSVP_NOT_FOUND);
        }
    }


    // 참석 여부에 따라 동반 인원과 식사 인원이 허용 범위에 있는지 확인
    private void validateCounts(
            Integer companionCount,
            Integer mealCount,
            boolean attending
    ) {
        // 참석이면 동반 인원과 식사 인원을 모두 입력
        if (attending) {
            if (companionCount == null || mealCount == null
                    || companionCount < 0 || mealCount < 0
            ) {
                throw new BusinessException(RsvpErrorCode.RSVP_INVALID_COUNTS);
            }

            // 식사 인원은 본인을 포함한 전체 참석 인원을 넘을 수 없음
            long totalGuests = 1L + companionCount;

            // 식사 인원이 전체 참석 인원을 초과할 경우
            if (totalGuests < mealCount) {
                throw new BusinessException(RsvpErrorCode.RSVP_INVALID_COUNTS);
            }

            return;
        }

        // 불참이면 두 인원은 각각 생략하거나 0으로 입력할 수 있음
        if ((companionCount != null && companionCount != 0)
                || (mealCount != null && mealCount != 0)) {
            throw new BusinessException(RsvpErrorCode.RSVP_INVALID_COUNTS);
        }
    }
}
