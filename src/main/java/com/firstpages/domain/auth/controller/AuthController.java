package com.firstpages.domain.auth.controller;

import com.firstpages.domain.auth.dto.response.AuthTokenPairDto;
import com.firstpages.domain.auth.service.AuthService;
import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.ResponseFactory;
import com.firstpages.global.response.code.CommonSuccessCode;
import com.firstpages.global.security.oauth2.OAuth2CookieManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 로그인 상태 유지와 토큰 재발급에 관한 API 요청을 처리
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/")
public class AuthController {

    private final AuthService authService;

    private final OAuth2CookieManager cookieManager;

    // Refresh Token을 이용한 JWT 재발급
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<Void>> reissue(
            @CookieValue(name = OAuth2CookieManager.REFRESH_TOKEN, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        // JWT 재발급
        AuthTokenPairDto tokens = authService.reissue(refreshToken);

        // 새 JWT를 HttpOnly 쿠키로 전달
        cookieManager.addTokenCookies(
                response,
                tokens.getAccessToken(),
                tokens.getRefreshToken()
        );

        return ResponseFactory.success(CommonSuccessCode.OK);
    }

    // 현재 브라우저의 로그인 상태 종료
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = OAuth2CookieManager.REFRESH_TOKEN, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        // Redis에 등록된 Refresh Token 폐기
        authService.logout(refreshToken);

        // 브라우저에 저장된 인증 쿠키 삭제
        cookieManager.clearTokenCookies(response);

        return ResponseFactory.success(CommonSuccessCode.OK);
    }
}
