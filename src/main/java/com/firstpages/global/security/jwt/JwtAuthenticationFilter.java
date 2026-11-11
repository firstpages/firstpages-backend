package com.firstpages.global.security.jwt;


import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.response.code.JwtErrorCode;
import com.firstpages.global.security.SecurityErrorResponseWriter;
import com.firstpages.global.security.oauth2.OAuth2CookieManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

// Access Token 쿠키를 검증하여 사용자를 인증하는 필터
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_REISSUE = "/api/auth/reissue";

    public static final String AUTH_LOGOUT = "/api/auth/logout";

    private final JwtProvider jwtProvider;

    private final SecurityErrorResponseWriter responseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 요청에 포함된 쿠키에서 Access Token 추출
        String token = resolveAccessToken(request);

        // 인증 쿠키가 없어도 공개 API는 접근 가능하도록 설정
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // JWT 서명, 만료시간, 토큰 유형을 검증
            Long userId = jwtProvider.getUserId(
                    token,
                    JwtTokenType.ACCESS
            );

            // 인증 객체 생성
            // Spring Security는 Authentication 객체를 통해 인증 상태를 관리
            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                    userId,
                    null,
                    Collections.emptyList()
            );

            // 인증 정보를 저장할 새로운 SecurityContext 생성
            SecurityContext context = SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

        } catch (BusinessException e) {

            // 유효하지 않은 JWT의 인증 정보가 남지 않도록 정리
            SecurityContextHolder.clearContext();

            responseWriter.write(
                    response,
                    e.getErrorCode()
            );

            return;
        }

        // 인증 객체가 등록된 상태로 다음 보안 필터 실행
        filterChain.doFilter(request, response);
    }

    // Access Token 없이 처리해야 하는 인증 API는 JWT 필터 생략
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return AUTH_REISSUE.equals(path) || AUTH_LOGOUT.equals(path);
    }

    // HTTP 요청에 포함된 쿠키에서 Access Token을 추출
    private String resolveAccessToken(HttpServletRequest request) {
        // 요청 쿠키 배열을 반환
        Cookie[] cookies = request.getCookies();

        // 쿠키가 비어있을 때
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (OAuth2CookieManager.ACCESS_TOKEN.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        // Access Token을 추출할 수 없을 때
        return null;
    }
}
