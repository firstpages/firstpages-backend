package com.firstpages.global.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// JWT를 HttpOnly 쿠키에 저장하여 클라이언트에 전달하고,
// 쿠키의 보안 속성과 만료시간을 공통으로 관리하는 컴포넌트
@Component
public class OAuth2CookieManager {

    // JWT 생성과 조회 과정에서 동일한 쿠키 이름을 사용하기 위한 상수
    public static final String ACCESS_TOKEN = "access_token";

    public static final String REFRESH_TOKEN = "refresh_token";

    private final boolean secure;

    private final Duration accessExpiration;

    private final Duration refreshExpiration;

    public OAuth2CookieManager(
            @Value("${app.oauth2.cookie-secure}") boolean secure,
            @Value("${jwt.access-expiration}") long accessExpirationMillis,
            @Value("${jwt.refresh-expiration}") long refreshExpirationMillis
    ) {
        this.secure = secure;
        this.accessExpiration = Duration.ofMillis(accessExpirationMillis);
        this.refreshExpiration = Duration.ofMillis(refreshExpirationMillis);
    }

    // 로그인 성공 시 Access Token과 Refresh Token을 각각 별도의 HttpOnly 쿠키로 응답에 추가
    public void addTokenCookies(
            HttpServletResponse response,
            String accessToken,
            String refreshToken
    ) {
        addCookie(
                response,
                ACCESS_TOKEN,
                accessToken,
                accessExpiration
        );

        addCookie(
                response,
                REFRESH_TOKEN,
                refreshToken,
                refreshExpiration
        );
    }

    // 쿠키의 보안 속성과 유효기간을 설정
    private void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            Duration maxAge
    ) {

        // HttpOnly 쿠키로 설정
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();

        // Set-Cookie 헤더는 여러 개 전달해야 하므로 addHeader를 사용
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    // 로그아웃 시 Access Token과 Refresh Token 쿠키 삭제
    public void clearTokenCookies(HttpServletResponse response) {
        // Access Token 쿠키 삭제
        clearCookie(response, ACCESS_TOKEN);

        // Refresh Token 쿠키 삭제
        clearCookie(response, REFRESH_TOKEN);
    }

    // 쿠키 만료시간을 0으로 설정하여 브라우저에서 삭제
    private void clearCookie(
            HttpServletResponse response,
            String cookieName
    ) {
        Cookie cookie = new Cookie(cookieName, "");

        // 기존 인증 쿠키와 동일한 경로를 사용
        cookie.setPath("/");

        // 쿠키를 즉시 만료
        cookie.setMaxAge(0);

        // JavaScript에서 접근할 수 없도록 설정
        cookie.setHttpOnly(true);

        response.addCookie(cookie);
    }
}
