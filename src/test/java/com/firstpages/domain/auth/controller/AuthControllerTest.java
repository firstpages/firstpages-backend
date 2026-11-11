
package com.firstpages.domain.auth.controller;

import com.firstpages.domain.auth.dto.response.AuthTokenPairDto;
import com.firstpages.domain.auth.exception.AuthErrorCode;
import com.firstpages.domain.auth.service.AuthService;
import com.firstpages.global.config.SecurityConfig;
import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.security.SecurityErrorResponseWriter;
import com.firstpages.global.security.jwt.JwtAccessDeniedHandler;
import com.firstpages.global.security.jwt.JwtAuthenticationEntryPoint;
import com.firstpages.global.security.jwt.JwtProvider;
import com.firstpages.global.security.oauth2.CustomOAuth2UserService;
import com.firstpages.global.security.oauth2.OAuth2CookieManager;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationFailureHandler;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// AuthController와 실제 Spring Security 필터 체인을 함께 검증
@WebMvcTest(
        controllers = AuthController.class,
        properties = "app.cors.allowed-origins=http://localhost:5173"
)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String REISSUE_URL = "/api/auth/reissue";
    private static final String LOGOUT_URL = "/api/auth/logout";

    private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";

    // Spring Security SPA 방식의 CSRF 쿠키와 헤더
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String CSRF_TOKEN = "test-csrf-token";

    @Autowired
    private MockMvc mockMvc;

    // 컨트롤러의 서비스 의존성 Mock
    @MockitoBean
    private AuthService authService;

    // 쿠키 갱신 및 삭제 요청을 검증하기 위한 Mock
    @MockitoBean
    private OAuth2CookieManager cookieManager;

    // 실제 SecurityConfig를 구성하기 위한 JWT 의존성
    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SecurityErrorResponseWriter responseWriter;

    @MockitoBean
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    // OAuth2 필터 체인의 의존성
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler successHandler;

    @MockitoBean
    private OAuth2AuthenticationFailureHandler failureHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    // 각 테스트 실행 전에 Mock 보안 예외 처리기 설정
    @BeforeEach
    void setUp() throws Exception {

        // 인증되지 않은 요청은 401 반환
        lenient().doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            return null;
        }).when(authenticationEntryPoint).commence(
                any(),
                any(),
                any()
        );

        // CSRF 실패 및 접근 거부는 403 반환
        lenient().doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);

            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN
            );

            return null;
        }).when(accessDeniedHandler).handle(
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("정상적인 Refresh Token으로 JWT를 재발급한다")
    void reissueSuccess() throws Exception {
        // given
        AuthTokenPairDto tokens = new AuthTokenPairDto(
                NEW_ACCESS_TOKEN,
                NEW_REFRESH_TOKEN
        );

        when(authService.reissue(OLD_REFRESH_TOKEN))
                .thenReturn(tokens);

        // when & then
        mockMvc.perform(
                        post(REISSUE_URL)
                                .cookie(
                                        refreshTokenCookie(),
                                        csrfCookie()
                                )
                                .header(
                                        CSRF_HEADER_NAME,
                                        CSRF_TOKEN
                                )
                )
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // Refresh Token이 서비스에 정상적으로 전달되었는지 확인
        verify(authService).reissue(OLD_REFRESH_TOKEN);

        // 새로운 JWT의 쿠키 설정 요청 여부 확인
        verify(cookieManager).addTokenCookies(
                any(),
                eq(NEW_ACCESS_TOKEN),
                eq(NEW_REFRESH_TOKEN)
        );
    }

    @Test
    @DisplayName("Refresh Token 쿠키가 없으면 401을 반환한다")
    void reissueWithoutRefreshToken() throws Exception {
        // given
        when(authService.reissue(null)).thenThrow(
                new BusinessException(
                        AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
                )
        );

        // when & then
        mockMvc.perform(
                        post(REISSUE_URL)
                                .cookie(csrfCookie())
                                .header(
                                        CSRF_HEADER_NAME,
                                        CSRF_TOKEN
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code")
                        .value("REFRESH_TOKEN_NOT_FOUND"));

        // Refresh Token이 없으므로 null 전달
        verify(authService).reissue(null);

        // 재발급에 실패했으므로 쿠키 갱신 요청 없음
        verifyNoInteractions(cookieManager);
    }

    @Test
    @DisplayName("CSRF 토큰 없이 재발급을 요청하면 403을 반환한다")
    void reissueWithoutCsrfToken() throws Exception {
        // when & then
        mockMvc.perform(
                        post(REISSUE_URL)
                                .cookie(refreshTokenCookie())
                )
                .andExpect(status().isForbidden());

        // CSRF 필터에서 차단되므로 서비스 호출 없음
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("이미 사용된 Refresh Token으로 재발급을 요청하면 401을 반환한다")
    void reissueWithReusedToken() throws Exception {
        // given
        when(authService.reissue(OLD_REFRESH_TOKEN)).thenThrow(
                new BusinessException(
                        AuthErrorCode.INVALID_REFRESH_TOKEN
                )
        );

        // when & then
        mockMvc.perform(
                        post(REISSUE_URL)
                                .cookie(
                                        refreshTokenCookie(),
                                        csrfCookie()
                                )
                                .header(
                                        CSRF_HEADER_NAME,
                                        CSRF_TOKEN
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"));

        // 이미 사용된 토큰은 쿠키를 갱신하지 않음
        verifyNoInteractions(cookieManager);
    }

    @Test
    @DisplayName("Access Token이 유효하지 않아도 재발급할 수 있다")
    void reissueWithInvalidAccessToken() throws Exception {
        // given
        AuthTokenPairDto tokens = new AuthTokenPairDto(
                NEW_ACCESS_TOKEN,
                NEW_REFRESH_TOKEN
        );

        when(authService.reissue(OLD_REFRESH_TOKEN))
                .thenReturn(tokens);

        // when & then
        mockMvc.perform(
                        post(REISSUE_URL)
                                .servletPath(REISSUE_URL)
                                .cookie(
                                        refreshTokenCookie(),
                                        csrfCookie()
                                )
                                .header(CSRF_HEADER_NAME, CSRF_TOKEN)
                )
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(AuthController.class));

        // 재발급 경로에서는 Access Token 인증을 수행하지 않음
        verifyNoInteractions(jwtProvider);

        verify(authService).reissue(OLD_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("로그아웃하면 Refresh Token을 폐기하고 쿠키 삭제를 요청한다")
    void logoutSuccess() throws Exception {
        // when & then
        mockMvc.perform(
                        post(LOGOUT_URL)
                                .cookie(
                                        refreshTokenCookie(),
                                        csrfCookie()
                                )
                                .header(
                                        CSRF_HEADER_NAME,
                                        CSRF_TOKEN
                                )
                )
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // Refresh Token 폐기를 위한 서비스 호출
        verify(authService).logout(OLD_REFRESH_TOKEN);

        // 인증 쿠키 삭제 요청
        verify(cookieManager).clearTokenCookies(any());
    }

    @Test
    @DisplayName("Refresh Token이 없어도 중복 로그아웃을 허용한다")
    void logoutWithoutRefreshToken() throws Exception {
        // when & then
        mockMvc.perform(
                        post(LOGOUT_URL)
                                .cookie(csrfCookie())
                                .header(
                                        CSRF_HEADER_NAME,
                                        CSRF_TOKEN
                                )
                )
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(AuthController.class));

        // Refresh Token이 없을 때는 null 전달
        verify(authService).logout(null);

        // 쿠키가 없어도 삭제 응답을 생성하도록 요청
        verify(cookieManager).clearTokenCookies(any());
    }

    @Test
    @DisplayName("CSRF 토큰 없이 로그아웃을 요청하면 403을 반환한다")
    void logoutWithoutCsrfToken() throws Exception {
        // when & then
        mockMvc.perform(
                        post(LOGOUT_URL)
                                .cookie(refreshTokenCookie())
                )
                .andExpect(status().isForbidden());

        // 서비스와 쿠키 매니저가 호출되면 안 됨
        verifyNoInteractions(authService, cookieManager);
    }

    @Test
    @DisplayName("Access Token이 유효하지 않아도 로그아웃할 수 있다")
    void logoutWithInvalidAccessToken() throws Exception {
        // when & then
        mockMvc.perform(
                        post(LOGOUT_URL)
                                .servletPath(LOGOUT_URL)
                                .cookie(
                                        refreshTokenCookie(),
                                        csrfCookie()
                                )
                                .header(CSRF_HEADER_NAME, CSRF_TOKEN)
                )
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(AuthController.class));

        // 로그아웃 경로에서는 JWT 인증 필터 실행 제외
        verifyNoInteractions(jwtProvider);

        // 정상 로그아웃 처리 확인
        verify(authService).logout(OLD_REFRESH_TOKEN);
        verify(cookieManager).clearTokenCookies(any());
    }

    // 테스트에 사용할 Refresh Token 쿠키 생성
    private Cookie refreshTokenCookie() {
        return new Cookie(
                OAuth2CookieManager.REFRESH_TOKEN,
                OLD_REFRESH_TOKEN
        );
    }

    // Spring Security SPA CSRF 검증에 사용할 쿠키 생성
    private Cookie csrfCookie() {
        Cookie cookie = new Cookie(
                CSRF_COOKIE_NAME,
                CSRF_TOKEN
        );

        cookie.setPath("/");
        return cookie;
    }
}
