
package com.firstpages.global.security.jwt;

import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.response.code.JwtErrorCode;
import com.firstpages.global.security.SecurityErrorResponseWriter;
import com.firstpages.global.security.oauth2.OAuth2CookieManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// JWT 인증 필터의 인증 및 예외 처리 검증
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final Long USER_ID = 9L;
    private static final String ACCESS_TOKEN = "valid-access-token";

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private SecurityErrorResponseWriter responseWriter;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        request.setServletPath("/api/users/me");
    }

    @AfterEach
    void tearDown() {
        // 테스트 간 인증 정보가 공유되지 않도록 정리
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 Access Token으로 사용자를 인증한다")
    void validAccessToken() throws Exception {
        // given
        request.setCookies(accessTokenCookie(ACCESS_TOKEN));

        when(jwtProvider.getUserId(
                ACCESS_TOKEN,
                JwtTokenType.ACCESS
        )).thenReturn(USER_ID);

        // when
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals(USER_ID, authentication.getPrincipal());

        // 인증 성공 시 다음 필터로 요청 전달
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(responseWriter);
    }

    @Test
    @DisplayName("Access Token 쿠키가 없으면 인증 없이 다음 필터로 전달한다")
    void missingAccessToken() throws Exception {
        // when
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(jwtProvider);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Refresh Token 쿠키만 있으면 Access Token 검증을 수행하지 않는다")
    void onlyRefreshToken() throws Exception {
        // given
        request.setCookies(
                new Cookie(
                        OAuth2CookieManager.REFRESH_TOKEN,
                        "refresh-token"
                )
        );

        // when
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(jwtProvider);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 Access Token은 인증 오류 응답을 작성한다")
    void invalidAccessToken() throws Exception {
        // given
        request.setCookies(accessTokenCookie("invalid-token"));

        when(jwtProvider.getUserId(
                "invalid-token",
                JwtTokenType.ACCESS
        )).thenThrow(
                new BusinessException(JwtErrorCode.INVALID_TOKEN)
        );

        // when
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(responseWriter).write(
                response,
                JwtErrorCode.INVALID_TOKEN
        );

        // 인증 실패 후 다음 필터를 실행하면 안 됨
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("만료된 Access Token은 인증 오류 응답을 작성한다")
    void expiredAccessToken() throws Exception {
        // given
        request.setCookies(accessTokenCookie("expired-token"));

        when(jwtProvider.getUserId(
                "expired-token",
                JwtTokenType.ACCESS
        )).thenThrow(
                new BusinessException(JwtErrorCode.EXPIRED_TOKEN)
        );

        // when
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        verify(responseWriter).write(
                response,
                JwtErrorCode.EXPIRED_TOKEN
        );

        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("기존 SecurityContext는 인증 실패 시 제거된다")
    void clearAuthenticationOnFailure() throws Exception {
        // given
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication
                        .UsernamePasswordAuthenticationToken(
                        USER_ID,
                        null,
                        java.util.Collections.emptyList()
                )
        );

        request.setCookies(accessTokenCookie("invalid-token"));

        when(jwtProvider.getUserId(
                "invalid-token",
                JwtTokenType.ACCESS
        )).thenThrow(
                new BusinessException(JwtErrorCode.INVALID_TOKEN)
        );

        // when
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );
    }

    @Test
    @DisplayName("재발급 요청은 JWT 인증 필터를 건너뛴다")
    void skipReissuePath() throws Exception {
        // given
        request.setMethod("POST");
        request.setRequestURI("/api/auth/reissue");
        request.setServletPath("/api/auth/reissue");

        request.setCookies(accessTokenCookie("invalid-token"));

        // when
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        verifyNoInteractions(jwtProvider, responseWriter);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("로그아웃 요청은 JWT 인증 필터를 건너뛴다")
    void skipLogoutPath() throws Exception {
        // given
        request.setMethod("POST");
        request.setRequestURI("/api/auth/logout");
        request.setServletPath("/api/auth/logout");

        request.setCookies(accessTokenCookie("invalid-token"));

        // when
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        verifyNoInteractions(jwtProvider, responseWriter);
        verify(filterChain).doFilter(request, response);
    }

    // 테스트 요청에 Access Token 쿠키 추가
    private Cookie accessTokenCookie(String token) {
        return new Cookie(
                OAuth2CookieManager.ACCESS_TOKEN,
                token
        );
    }
}
