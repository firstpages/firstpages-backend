package com.firstpages.global.security.jwt;

import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.response.code.JwtErrorCode;
import com.firstpages.global.security.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtProvider jwtProvider;
    private SecurityErrorResponseWriter responseWriter;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtProvider = mock(JwtProvider.class);
        responseWriter = mock(SecurityErrorResponseWriter.class);

        filter = new JwtAuthenticationFilter(
                jwtProvider,
                responseWriter
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 정상_토큰이면_인증_정보를_등록한다() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(
                "Authorization",
                "Bearer valid-token"
        );

        when(jwtProvider.getUserId(
                "valid-token",
                JwtTokenType.ACCESS
        )).thenReturn(1L);

        FilterChain chain = (req, res) -> {
            // 필터 체인 내부에서 인증 정보 확인
            Authentication authentication = SecurityContextHolder
                    .getContext()
                    .getAuthentication();

            assertThat(authentication).isNotNull();
            assertThat(authentication.isAuthenticated()).isTrue();
            assertThat(authentication.getPrincipal()).isEqualTo(1L);
        };

        // when
        filter.doFilter(request, response, chain);

        // then
        verify(jwtProvider).getUserId(
                "valid-token",
                JwtTokenType.ACCESS
        );

        verifyNoInteractions(responseWriter);
    }

    @Test
    void 토큰이_없으면_인증_정보를_등록하지_않는다()
            throws Exception {

        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            Authentication authentication =
                    SecurityContextHolder.getContext()
                            .getAuthentication();

            assertThat(authentication).isNull();
        };

        // when
        filter.doFilter(request, response, chain);

        // then
        verifyNoInteractions(jwtProvider, responseWriter);
    }

    @Test
    void 만료된_토큰이면_오류_응답을_작성한다()
            throws Exception {

        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        request.addHeader(
                "Authorization",
                "Bearer expired-token"
        );

        when(jwtProvider.getUserId(
                "expired-token",
                JwtTokenType.ACCESS
        )).thenThrow(
                new BusinessException(JwtErrorCode.EXPIRED_TOKEN)
        );

        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, chain);

        // then
        verify(responseWriter).write(
                response,
                JwtErrorCode.EXPIRED_TOKEN
        );

        verifyNoInteractions(chain);
    }

    @Test
    void Bearer_형식이_아니면_인증에_실패한다()
            throws Exception {

        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        request.addHeader(
                "Authorization",
                "invalid-token"
        );

        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, chain);

        // then
        verify(responseWriter).write(
                response,
                JwtErrorCode.INVALID_TOKEN
        );

        verifyNoInteractions(jwtProvider, chain);
    }
}