package com.firstpages.domain.invitation.controller;

import com.firstpages.domain.invitation.dto.request.InvitationCreateRequestDto;
import com.firstpages.domain.invitation.dto.request.InvitationPublishRequestDto;
import com.firstpages.domain.invitation.dto.request.InvitationUpdateRequestDto;
import com.firstpages.domain.invitation.dto.response.*;
import com.firstpages.domain.invitation.service.InvitationService;
import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.ResponseFactory;
import com.firstpages.global.response.code.CommonSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 청첩장 생성 및 조회 API의 HTTP 요청과 응답을 처리
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    // 사용자의 청첩장을 생성
    @PostMapping
    public ResponseEntity<ApiResponse<InvitationCreateResponseDto>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody InvitationCreateRequestDto requestDto
    ) {
        InvitationCreateResponseDto responseDto = invitationService.create(userId, requestDto);

        return ResponseFactory.success(
                CommonSuccessCode.CREATED,
                responseDto
        );
    }

    // 사용자가 소유한 전체 청첩장 목록을 반환
    @GetMapping
    public ResponseEntity<ApiResponse<List<InvitationListResponseDto>>> getInvitations(
            @AuthenticationPrincipal Long userId
    ) {
        List<InvitationListResponseDto> responseDto = invitationService.getInvitations(userId);

        return ResponseFactory.success(
                CommonSuccessCode.OK,
                responseDto
        );
    }

    // 사용자가 소유한 청첩장의 편집용 상세 정보를 반환
    @GetMapping("/{invitationId}")
    public ResponseEntity<ApiResponse<InvitationDetailResponseDto>> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long invitationId
    ) {
        InvitationDetailResponseDto responseDto = invitationService.getDetail(
                userId,
                invitationId
        );

        return ResponseFactory.success(
                CommonSuccessCode.OK,
                responseDto
        );
    }

    // 작성본 저장 후 청첩장 ID와 현재 버전을 조회하여 반환
    @PutMapping("/{invitationId}")
    public ResponseEntity<ApiResponse<InvitationUpdateResponseDto>> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long invitationId,
            @Valid @RequestBody InvitationUpdateRequestDto requestDto
    ) {
        // 제목과 작성본 저장 및 트랜잭션 커밋
        invitationService.update(
                userId,
                invitationId,
                requestDto
        );

        InvitationUpdateResponseDto responseDto = invitationService.getUpdateResult(
                userId,
                invitationId
        );

        return ResponseFactory.success(
                CommonSuccessCode.OK,
                responseDto
        );
    }

    // 청첩장 공개 요청을 처리하고 현재 공개 결과를 반환
    @PostMapping("/{invitationId}/publish")
    public ResponseEntity<ApiResponse<InvitationPublishResponseDto>> publish(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long invitationId,
            @Valid @RequestBody InvitationPublishRequestDto requestDto
    ) {
        // 공개본과 부모 청첩장의 변경을 같은 트랜잭션에서 커밋
        // 작성본을 공개본으로 전환
        invitationService.publish(
                userId,
                invitationId,
                requestDto
        );

        // 커밋 이후 조회한 공개본 결과
        InvitationPublishResponseDto responseDto = invitationService.getPublishResult(
                userId,
                invitationId
        );

        return ResponseFactory.success(
                CommonSuccessCode.OK,
                responseDto
        );
    }
}
