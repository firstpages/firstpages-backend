package com.firstpages.domain.rsvp.controller;

import com.firstpages.domain.rsvp.dto.request.RsvpCreateRequestDto;
import com.firstpages.domain.rsvp.service.RsvpService;
import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.ResponseFactory;
import com.firstpages.global.response.code.CommonSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 하객의 공개 청첩장 참석 응답 요청을 처리
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/invitations/{publicId}/rsvps")
public class PublicRsvpController {

    // 하객의 참석 응답 제출을 처리
    private final RsvpService rsvpService;

    // 요청 본문을 검증하고 응답 저장이 완료되면 201을 반환
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @PathVariable String publicId,
            @Valid @RequestBody RsvpCreateRequestDto requestDto
    ) {

        // 하객의 참석 응답을 생성
        rsvpService.create(
                publicId,
                requestDto
        );

        return ResponseFactory.success(
                CommonSuccessCode.CREATED
        );
    }
}
