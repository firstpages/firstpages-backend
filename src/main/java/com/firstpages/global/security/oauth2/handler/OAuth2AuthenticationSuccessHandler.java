package com.firstpages.global.security.oauth2.handler;

import com.firstpages.domain.auth.service.RefreshTokenService;
import com.firstpages.global.security.jwt.JwtProvider;
import com.firstpages.global.security.oauth2.CustomOAuth2User;
import com.firstpages.global.security.oauth2.OAuth2CookieManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// OAuth2 로그인 성공 후 JWT 발급과 인증 완료 처리를 담당
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    private final OAuth2CookieManager oAuth2CookieManager;

    private RefreshTokenService refreshTokenService;

    public OAuth2AuthenticationSuccessHandler(
            JwtProvider jwtProvider,
            OAuth2CookieManager oAuth2CookieManager,
            RefreshTokenService refreshTokenService,
            @Value("${app.oauth2.success-redirect-uri}") String successRedirectUri
    ) {
        this.jwtProvider = jwtProvider;
        this.oAuth2CookieManager = oAuth2CookieManager;
        this.refreshTokenService = refreshTokenService;

        // 로그인 성공 이후 이동할 기본 URL
        setDefaultTargetUrl(successRedirectUri);

        // 저장된 요청이나 URL 파라미터와 관계없이 지정한 URL로 이동
        setAlwaysUseDefaultTargetUrl(true);
    }

    // OAuth2 로그인 성공 시 토큰 생성, Redis 저장, 쿠키 발급 처리
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // OAuth2 Principal에서 서비스 내부 사용자 ID 조회
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        Long userId = oAuth2User.getUserId();

        // 자체 JWT 발급
        String accessToken = jwtProvider.createAccessToken(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        // Refresh Token을 쿠키로 전달하기 전에 Redis에 저장
        refreshTokenService.save(userId, refreshToken);

        // JWT를 HttpOnly 쿠키에 저장하여 클라이언트로 전달
        oAuth2CookieManager.addTokenCookies(
                response,
                accessToken,
                refreshToken
        );

        // OAuth2 인증 과정에서 사용한 임시 HTTP 세션 무효화
        HttpSession session = request.getSession(false);

        // 기존 세션 제거
        if (session != null) {
            // 해당 HTTP 세션을 무효화
            session.invalidate();
        }

        // 현재 스레드의 SecurityContext 정리
        SecurityContextHolder.clearContext();

        // 부모 클래스의 로그인 성공 처리 호출
        super.onAuthenticationSuccess(
                request,
                response,
                authentication
        );
    }
}
