package com.firstpages.domain.user.controller;

import com.firstpages.domain.user.dto.response.UserMeResponseDto;
import com.firstpages.domain.user.service.UserService;
import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.ResponseFactory;
import com.firstpages.global.response.code.CommonSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 사용자 관련 API 요청을 처리하는 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // JWT로 인증된 현재 사용자의 정보를 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponseDto>> getMyInfo(
            @AuthenticationPrincipal Long userId
    ) {
        // SecurityContext에 저장된 사용자 ID로 회원 정보 조회
        UserMeResponseDto responseDto = userService.getMyInfo(userId);

        return ResponseFactory.success(CommonSuccessCode.OK, responseDto);
    }


}
