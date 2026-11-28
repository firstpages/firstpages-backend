
package com.firstpages.global.security.oauth2.handler;

import com.firstpages.domain.auth.dto.response.AuthTokenPairDto;
import com.firstpages.domain.auth.service.LoginTokenIssuer;
import com.firstpages.global.security.oauth2.CustomOAuth2User;
import com.firstpages.global.security.oauth2.OAuth2CookieManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// OAuth2 로그인 성공 시 토큰 발급 위임, 쿠키 설정 및 리다이렉트 검증
@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    private static final Long USER_ID = 9L;

    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String REFRESH_TOKEN = "test-refresh-token";

    private static final String REDIRECT_URL =
            "http://localhost:5173/auth/callback";

    @Mock
    private LoginTokenIssuer tokenIssuer;

    @Mock
    private OAuth2AuthenticationFailureHandler failureHandler;

    @Mock
    private OAuth2CookieManager cookieManager;

    @Mock
    private Authentication authentication;

    @Mock
    private CustomOAuth2User customOAuth2User;

    private OAuth2AuthenticationSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2AuthenticationSuccessHandler(
                tokenIssuer,
                cookieManager,
                failureHandler,
                REDIRECT_URL
        );
    }

    @Test
    @DisplayName("로그인 성공 시 JWT를 쿠키에 설정한다")
    void addCookiesOnSuccess() throws Exception {
        // given
        setUpAuthentication();

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        // when
        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        // then
        verify(cookieManager).addTokenCookies(
                response,
                ACCESS_TOKEN,
                REFRESH_TOKEN
        );
    }

    @Test
    @DisplayName("로그인 성공 시 프론트엔드 콜백 페이지로 리다이렉트한다")
    void redirectToFrontendCallback() throws Exception {
        // given
        setUpAuthentication();

        // when
        MockHttpServletResponse response =
                performLoginSuccess();

        // then
        assertEquals(302, response.getStatus());

        assertEquals(
                REDIRECT_URL,
                response.getHeader("Location")
        );
    }

    @Test
    @DisplayName("JWT 원문이 리다이렉트 URL에 노출되지 않는다")
    void tokensAreNotExposedInRedirect() throws Exception {
        // given
        setUpAuthentication();

        // when
        MockHttpServletResponse response =
                performLoginSuccess();

        String location = response.getHeader("Location");

        // then
        assertNotNull(location);

        assertFalse(location.contains(ACCESS_TOKEN));
        assertFalse(location.contains(REFRESH_TOKEN));

        // 토큰이 URL 쿼리 파라미터로 전달되지 않아야 함
        assertFalse(location.contains("access_token="));
        assertFalse(location.contains("refresh_token="));
    }

    @Test
    @DisplayName("토큰 발급을 마친 뒤 인증 쿠키를 설정한다")
    void issuesTokensBeforeAddingCookies() throws Exception {
        setUpAuthentication();

        MockHttpServletResponse response = performLoginSuccess();

        InOrder order = inOrder(tokenIssuer, cookieManager);
        order.verify(tokenIssuer).issue(USER_ID);
        order.verify(cookieManager).addTokenCookies(response, ACCESS_TOKEN, REFRESH_TOKEN);
    }

    @Test
    @DisplayName("발급 직전에 탈퇴한 회원은 로그인 실패 핸들러로 전달한다")
    void delegatesFailureWhenUserWasDeleted() throws Exception {
        when(authentication.getPrincipal()).thenReturn(customOAuth2User);
        when(customOAuth2User.getUserId()).thenReturn(USER_ID);
        OAuth2AuthenticationException exception =
                new OAuth2AuthenticationException(new OAuth2Error("account_unavailable"));
        when(tokenIssuer.issue(USER_ID)).thenThrow(exception);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(failureHandler).onAuthenticationFailure(request, response, exception);
        verifyNoInteractions(cookieManager);
        assertNull(response.getRedirectedUrl());
    }

    // OAuth 사용자 정보와 토큰 발급 결과 준비
    private void setUpAuthentication() {
        when(authentication.getPrincipal()).thenReturn(customOAuth2User);
        when(customOAuth2User.getUserId()).thenReturn(USER_ID);
        when(tokenIssuer.issue(USER_ID)).thenReturn(new AuthTokenPairDto(ACCESS_TOKEN, REFRESH_TOKEN));
    }

    // 로그인 성공 핸들러를 호출하고 HTTP 응답 반환
    private MockHttpServletResponse performLoginSuccess()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        return response;
    }
}
