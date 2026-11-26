package com.firstpages.domain.guestbook.controller;

import com.firstpages.domain.guestbook.dto.request.GuestbookCreateRequestDto;
import com.firstpages.domain.guestbook.service.GuestbookService;
import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.ResponseFactory;
import com.firstpages.global.response.code.CommonSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 하객의 방명록 작성 요청을 처리
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/invitations/{publicId}/guestbooks")
public class PublicGuestbookController {

    // 공개 청첩장의 방명록 메시지 등록을 처리
    private final GuestbookService guestbookService;

    // 하객의 축하 메시지를 저장하고 작성 완료를 응답
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @PathVariable String publicId,
            @Valid @RequestBody GuestbookCreateRequestDto requestDto
    ) {

        guestbookService.create(
                publicId,
                requestDto
        );

        return ResponseFactory.success(
                CommonSuccessCode.CREATED
        );
    }
}
