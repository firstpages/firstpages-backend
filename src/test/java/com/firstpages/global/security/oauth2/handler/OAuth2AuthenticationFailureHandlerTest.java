
package com.firstpages.global.security.oauth2.handler;

import com.firstpages.global.security.oauth2.OAuth2CookieManager;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.junit.jupiter.api.Assertions.*;

// OAuth2 로그인 실패 시 응답 처리 검증
class OAuth2AuthenticationFailureHandlerTest {

    private OAuth2AuthenticationFailureHandler failureHandler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String FAILURE_REDIRECT_URL =
            "http://localhost:5173/auth/callback?error=oauth2";

    @BeforeEach
    void setUp() {
        // 테스트용 로그인 실패 리다이렉트 URL 설정
        failureHandler = new OAuth2AuthenticationFailureHandler(
                FAILURE_REDIRECT_URL
        );

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("카카오 로그인 실패 시 리다이렉트 응답을 반환한다")
    void redirectOnAuthenticationFailure() throws Exception {
        // given
        AuthenticationException exception =
                createOAuth2Exception("access_denied");

        // when
        failureHandler.onAuthenticationFailure(
                request,
                response,
                exception
        );

        // then
        assertEquals(
                HttpServletResponse.SC_FOUND,
                response.getStatus()
        );

        assertNotNull(response.getHeader("Location"));
    }

    @Test
    @DisplayName("인증 실패 시 Access Token 및 Refresh Token을 발급하지 않는다")
    void noAuthenticationCookiesOnFailure() throws Exception {
        // given
        AuthenticationException exception =
                createOAuth2Exception("access_denied");

        // when
        failureHandler.onAuthenticationFailure(
                request,
                response,
                exception
        );

        // then
        assertFalse(hasCookie(
                OAuth2CookieManager.ACCESS_TOKEN
        ));

        assertFalse(hasCookie(
                OAuth2CookieManager.REFRESH_TOKEN
        ));
    }

    @Test
    @DisplayName("리다이렉트 URL에는 민감한 인증 정보가 포함되지 않는다")
    void noSensitiveInformationInRedirect() throws Exception {
        // given
        AuthenticationException exception =
                createOAuth2Exception("access_denied");

        // when
        failureHandler.onAuthenticationFailure(
                request,
                response,
                exception
        );

        // then
        String location = response.getHeader("Location");

        assertNotNull(location);

        assertFalse(location.contains("access_token="));
        assertFalse(location.contains("refresh_token="));
        assertFalse(location.contains("client_secret="));
    }

    // OAuth2 로그인 실패 예외 생성
    private AuthenticationException createOAuth2Exception(
            String errorCode
    ) {
        OAuth2Error error = new OAuth2Error(
                errorCode,
                "OAuth2 authentication failed",
                null
        );

        return new OAuth2AuthenticationException(error);
    }

    // 응답에 지정한 이름의 쿠키가 있는지 검사
    private boolean hasCookie(String cookieName) {
        return response.getHeaders("Set-Cookie")
                .stream()
                .anyMatch(header ->
                        header.startsWith(cookieName + "=")
                );
    }
}
