package com.firstpages.global.security.jwt;


import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.response.code.JwtErrorCode;
import com.firstpages.global.security.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final SecurityErrorResponseWriter responseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // HTTP 헤더의 Authorization 키의 값을 추출(Bearer eyJhbGciOiJIUzI1NiJ9...)
        String authorization = request.getHeader(
                HttpHeaders.AUTHORIZATION
        );

        // Authorization 헤더가 없으면 다음 필터로 전달
        if (authorization == null || authorization.isBlank()) {
            // 일단 다음 필터로 요청과 응답을 전달
            filterChain.doFilter(request, response);
            return;
        }

        // Bearer 형식이 아니면 인증 실패
        if (!authorization.startsWith(BEARER_PREFIX)) {
            responseWriter.write(
                    response,
                    JwtErrorCode.INVALID_TOKEN
            );

            return;
        }

        // "Bearer " 문자열을 제거하여 JWT만 추출
        String token = authorization.substring(BEARER_PREFIX.length());

        if (token.isBlank()) {
            responseWriter.write(
                    response,
                    JwtErrorCode.INVALID_TOKEN
            );

            return;
        }

        Long userId;

        try {
            userId = jwtProvider.getUserId(
                    token,
                    JwtTokenType.ACCESS
            );
        } catch (BusinessException e) {
            SecurityContextHolder.clearContext();

            responseWriter.write(
                    response,
                    e.getErrorCode()
            );

            return;
        }

        // 인증 객체 생성
        // Spring Security는 Authentication 객체를 통해 인증 상태를 관리
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userId,
                null,
                Collections.emptyList()
        );

        // 인증 정보를 저장할 새로운 SecurityContext 생성
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // 인증 객체 저장
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        // 다음 필터로 전달
        filterChain.doFilter(request, response);
    }
}
