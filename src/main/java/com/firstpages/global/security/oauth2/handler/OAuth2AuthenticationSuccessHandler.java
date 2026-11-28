package com.firstpages.global.security.oauth2.handler;

import com.firstpages.domain.auth.dto.response.AuthTokenPairDto;
import com.firstpages.domain.auth.service.LoginTokenIssuer;
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
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// OAuth2 로그인 성공 후 JWT 발급과 인증 완료 처리를 담당
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // 서비스 토큰 발급 계약
    private final LoginTokenIssuer tokenIssuer;

    // 발급된 토큰을 브라우저 쿠키로 전달하는 컴포넌트
    private final OAuth2CookieManager cookieManager;

    // 로그인 완료 단계의 인증 실패를 처리하는 핸들러
    private final OAuth2AuthenticationFailureHandler failureHandler;

    public OAuth2AuthenticationSuccessHandler(
            LoginTokenIssuer tokenIssuer,
            OAuth2CookieManager cookieManager,
            OAuth2AuthenticationFailureHandler failureHandler,
            @Value("${app.oauth2.success-redirect-uri}") String successRedirectUri
    ) {
        // 토큰 발급과 HTTP 응답 처리에 사용할 의존성 저장
        this.tokenIssuer = tokenIssuer;
        this.cookieManager = cookieManager;
        this.failureHandler = failureHandler;

        // 로그인 성공 시 설정된 주소로 이동
        setDefaultTargetUrl(successRedirectUri);
        setAlwaysUseDefaultTargetUrl(true);
    }

    // OAuth2 로그인 성공 시 토큰 생성, Redis 저장, 쿠키 발급 처리
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        // OAuth 인증 주체에서 내부 회원 ID 조회
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();

        // 발급 결과를 쿠키에 전달하기 위한 변수
        AuthTokenPairDto tokens;

        try {
            // 회원 존재를 재확인하고 서비스 토큰 발급
            tokens = tokenIssuer.issue(principal.getUserId());
        } catch (OAuth2AuthenticationException exception) {
            // 인증 컨텍스트를 정리하고 기존 로그인 실패 처리로 위임
            SecurityContextHolder.clearContext();

            failureHandler.onAuthenticationFailure(
                    request,
                    response,
                    exception
            );

            return;
        }

        // 발급이 끝난 토큰을 브라우저 인증 쿠키로 전달
        cookieManager.addTokenCookies(
                response,
                tokens.getAccessToken(),
                tokens.getRefreshToken()
        );

        // OAuth 로그인에 사용한 임시 세션 종료
        HttpSession session = request.getSession(false);

        if (session != null) {
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
