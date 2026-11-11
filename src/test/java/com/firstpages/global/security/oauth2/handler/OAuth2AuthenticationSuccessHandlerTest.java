
package com.firstpages.global.security.oauth2.handler;

import com.firstpages.domain.auth.service.RefreshTokenService;
import com.firstpages.global.security.jwt.JwtProvider;
import com.firstpages.global.security.oauth2.CustomOAuth2User;
import com.firstpages.global.security.oauth2.OAuth2CookieManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// OAuth2 로그인 성공 시 JWT 발급, 저장, 쿠키 설정 및 리다이렉트 검증
@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    private static final Long USER_ID = 9L;

    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String REFRESH_TOKEN = "test-refresh-token";

    private static final String REDIRECT_URL =
            "http://localhost:5173/auth/callback";

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

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
                jwtProvider,
                cookieManager,
                refreshTokenService,
                REDIRECT_URL
        );
    }

    @Test
    @DisplayName("로그인 성공 시 Access Token과 Refresh Token을 발급한다")
    void issueTokensOnSuccess() throws Exception {
        // given
        setUpAuthentication();

        // when
        performLoginSuccess();

        // then
        verify(jwtProvider).createAccessToken(USER_ID);
        verify(jwtProvider).createRefreshToken(USER_ID);
    }

    @Test
    @DisplayName("로그인 성공 시 Refresh Token을 Redis에 저장한다")
    void saveRefreshTokenOnSuccess() throws Exception {
        // given
        setUpAuthentication();

        // when
        performLoginSuccess();

        // then
        verify(refreshTokenService).save(
                USER_ID,
                REFRESH_TOKEN
        );

        // 저장은 한 번만 수행되어야 함
        verify(refreshTokenService, times(1)).save(
                USER_ID,
                REFRESH_TOKEN
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
    @DisplayName("로그인 성공 시 Refresh Token 저장 후 쿠키를 설정한다")
    void saveRefreshTokenBeforeAddingCookies() throws Exception {
        // given
        setUpAuthentication();

        // when
        performLoginSuccess();

        // then
        var inOrder = inOrder(
                jwtProvider,
                refreshTokenService,
                cookieManager
        );

        // JWT 발급
        inOrder.verify(jwtProvider)
                .createAccessToken(USER_ID);

        inOrder.verify(jwtProvider)
                .createRefreshToken(USER_ID);

        // Refresh Token 저장
        inOrder.verify(refreshTokenService)
                .save(USER_ID, REFRESH_TOKEN);

        // 쿠키 설정
        inOrder.verify(cookieManager)
                .addTokenCookies(
                        any(),
                        eq(ACCESS_TOKEN),
                        eq(REFRESH_TOKEN)
                );
    }

    // OAuth2 로그인 성공 시 반환되는 사용자 정보 설정
    private void setUpAuthentication() {
        when(authentication.getPrincipal())
                .thenReturn(customOAuth2User);

        when(customOAuth2User.getUserId())
                .thenReturn(USER_ID);

        when(jwtProvider.createAccessToken(USER_ID))
                .thenReturn(ACCESS_TOKEN);

        when(jwtProvider.createRefreshToken(USER_ID))
                .thenReturn(REFRESH_TOKEN);
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
