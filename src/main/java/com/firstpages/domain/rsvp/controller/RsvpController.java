package com.firstpages.domain.rsvp.controller;

import com.firstpages.domain.rsvp.dto.response.RsvpListResponseDto;
import com.firstpages.domain.rsvp.service.RsvpService;
import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.ResponseFactory;
import com.firstpages.global.response.code.CommonSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 청첩장 소유자의 참석 응답 관리 요청을 처리하는 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invitations/{invitationId}/rsvps")
public class RsvpController {

    // Rsvp 서비스
    private final RsvpService rsvpService;

    // 요청한 참석 응답 페이지와 신랑, 신부 측별 집계를 반환
    @GetMapping
    public ResponseEntity<ApiResponse<RsvpListResponseDto>> getRsvps(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long invitationId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {

        // 작성된 참석여부 모두 조회
        RsvpListResponseDto responseDto = rsvpService.getRsvps(
                userId,
                invitationId,
                page,
                size
        );

        return ResponseFactory.success(
                CommonSuccessCode.OK,
                responseDto
        );
    }

}
