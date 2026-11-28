package com.firstpages.domain.user.controller;

import com.firstpages.domain.user.dto.response.UserMeResponseDto;
import com.firstpages.domain.user.service.UserService;
import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.ResponseFactory;
import com.firstpages.global.response.code.CommonSuccessCode;
import com.firstpages.global.security.oauth2.OAuth2CookieManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 사용자 관련 API 요청을 처리하는 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    // 사용자 서비스
    private final UserService userService;

    // OAuth 쿠키 매니저
    private final OAuth2CookieManager oAuth2CookieManager;

    // JWT로 인증된 현재 사용자의 정보를 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponseDto>> getMyInfo(
            @AuthenticationPrincipal Long userId
    ) {
        // SecurityContext에 저장된 사용자 ID로 회원 정보 조회
        UserMeResponseDto responseDto = userService.getMyInfo(userId);

        return ResponseFactory.success(
                CommonSuccessCode.OK,
                responseDto
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal Long userId,
            HttpServletResponse response
    ) {
        // 회원탈퇴
        userService.withdraw(userId);

        // 현재 브라우저의 인증 쿠키 삭제
        oAuth2CookieManager.clearTokenCookies(response);

        // 탈퇴 완료 응답 반환
        return ResponseFactory.success(CommonSuccessCode.OK);
    }
}
