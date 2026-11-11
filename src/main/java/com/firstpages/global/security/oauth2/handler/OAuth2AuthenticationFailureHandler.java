package com.firstpages.global.security.oauth2.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// OAuth2 로그인 실패 시 실행되는 핸들러
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.oauth2.failure-redirect-uri}") String failureRedirectUri
    ) {
        // 로그인 실패 시 이동할 URL 설정
        setDefaultFailureUrl(failureRedirectUri + "?error=oauth2_failed");

        // 실패 예외를 저장하기 위한 새로운 HttpSession을 만들지 않음
        setAllowSessionCreation(false);
    }

    // OAuth2 인증 실패 시 Spring Security가 호출
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        // 기존 OAuth2 로그인 세션이 있으면 무효화
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        // 현재 스레드의 인증 컨텍스트 정리
        SecurityContextHolder.clearContext();

        // 부모 클래스의 실패 처리 및 리다이렉트 실행
        super.onAuthenticationFailure(request, response, exception);
    }
}
