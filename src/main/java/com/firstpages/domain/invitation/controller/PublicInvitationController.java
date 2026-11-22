package com.firstpages.domain.invitation.controller;

import com.firstpages.domain.invitation.dto.response.PublicInvitationResponseDto;
import com.firstpages.domain.invitation.service.InvitationService;
import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.ResponseFactory;
import com.firstpages.global.response.code.CommonSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 하객용 공개 청첩장 조회 요청을 처리
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/invitations")
public class PublicInvitationController {

    private final InvitationService invitationService;

    // 공유 식별자로 공개 청첩장을 조회해 공통 응답으로 반환
    @GetMapping("/{publicId}")
    public ResponseEntity<ApiResponse<PublicInvitationResponseDto>> getPublicInvitation(
            @PathVariable String publicId
    ) {
        PublicInvitationResponseDto responseDto = invitationService.getPublicInvitation(publicId);

        return ResponseFactory.success(
                CommonSuccessCode.OK,
                responseDto
        );
    }
}