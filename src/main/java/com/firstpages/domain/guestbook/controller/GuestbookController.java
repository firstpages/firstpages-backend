package com.firstpages.domain.guestbook.controller;

import com.firstpages.domain.guestbook.dto.response.GuestbookItemResponseDto;
import com.firstpages.domain.guestbook.service.GuestbookService;
import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.PageResponse;
import com.firstpages.global.response.ResponseFactory;
import com.firstpages.global.response.code.CommonSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 청첩장 소유자의 방명록 관리 요청을 처리
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invitations/{invitationId}/guestbooks")
public class GuestbookController {

    // 방명록 서비스
    private final GuestbookService guestbookService;

    // 요청한 페이지의 메시지와 전체 페이지 수를 반환
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<GuestbookItemResponseDto>>> getGuestbooks(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long invitationId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {

        // 작성된 방명록 리스트
        Page<GuestbookItemResponseDto> responseDto = guestbookService.getGuestbooks(
                userId,
                invitationId,
                page,
                size
        );

        return ResponseFactory.pageSuccess(
                CommonSuccessCode.OK,
                responseDto
        );
    }

    // 로그인한 소유자의 방명록 메시지를 삭제하고 성공 응답을 반환
    @DeleteMapping("/{guestbookId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long invitationId,
            @PathVariable Long guestbookId
    ) {

        guestbookService.delete(
                userId,
                invitationId,
                guestbookId
        );

        return ResponseFactory.success(
                CommonSuccessCode.OK
        );
    }

}
