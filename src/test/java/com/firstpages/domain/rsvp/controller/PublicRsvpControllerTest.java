package com.firstpages.domain.rsvp.controller;

import com.firstpages.domain.rsvp.dto.request.RsvpCreateRequestDto;
import com.firstpages.domain.rsvp.exception.RsvpErrorCode;
import com.firstpages.domain.rsvp.service.RsvpService;
import com.firstpages.global.config.SecurityConfig;
import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.exception.GlobalExceptionHandler;
import com.firstpages.global.security.CsrfController;
import com.firstpages.global.security.SecurityErrorResponseWriter;
import com.firstpages.global.security.jwt.JwtAccessDeniedHandler;
import com.firstpages.global.security.jwt.JwtAuthenticationEntryPoint;
import com.firstpages.global.security.jwt.JwtProvider;
import com.firstpages.global.security.oauth2.CustomOAuth2UserService;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationFailureHandler;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 공개 작성 API의 보안 규칙, 요청 검증, HTTP 응답을 검증한다.
@WebMvcTest(
        controllers = {PublicRsvpController.class, CsrfController.class},
        properties = "app.cors.allowed-origins=http://localhost:5173"
)
@Import({
        SecurityConfig.class, GlobalExceptionHandler.class, SecurityErrorResponseWriter.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class
})
@DisplayName("하객용 RSVP 제출 API")
class PublicRsvpControllerTest {

    // 하객용 RSVP 제출 경로
    private static final String URL = "/api/public/invitations/{publicId}/rsvps";

    // URL에 전달할 공유 식별자
    private static final String PUBLIC_ID = "0123456789abcdef0123456789abcdef";

    // 실제 MVC와 보안 필터를 거쳐 요청을 실행한다.
    @Autowired
    private MockMvc mockMvc;

    // 서비스 호출 인자와 도메인 예외 응답을 확인한다.
    @MockitoBean
    private RsvpService rsvpService;

    // JWT 보안 필터의 의존성을 제공한다.
    @MockitoBean
    private JwtProvider jwtProvider;

    // OAuth2 보안 필터의 사용자 조회 의존성을 제공한다.
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    // OAuth2 인증 성공 핸들러의 의존성을 제공한다.
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler successHandler;

    // OAuth2 인증 실패 핸들러의 의존성을 제공한다.
    @MockitoBean
    private OAuth2AuthenticationFailureHandler failureHandler;

    // OAuth2 클라이언트 설정의 의존성을 제공한다.
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    // 로그인 쿠키 없이 CSRF 토큰으로 요청하고 전화번호 앞자리 0이 유지되는지 확인한다.
    @Test
    @DisplayName("로그인 없이 RSVP를 제출하면 201을 반환하고 전화번호 앞자리 0을 유지한다")
    void createsResponseWithoutAuthentication() throws Exception {
        submit("0123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"));

        ArgumentCaptor<RsvpCreateRequestDto> request =
                ArgumentCaptor.forClass(RsvpCreateRequestDto.class);
        verify(rsvpService).create(eq(PUBLIC_ID), request.capture());
        assertThat(request.getValue().getPhoneLastFour()).isEqualTo("0123");
        assertThat(request.getValue().getCompanionCount()).isEqualTo(2);
        assertThat(request.getValue().getMealCount()).isEqualTo(1);
    }

    // 네 자리 형식이 틀리면 서비스 호출 전에 요청을 거절한다.
    @Test
    @DisplayName("전화번호 뒷자리가 숫자 네 자리가 아니면 400을 반환한다")
    void rejectsInvalidPhoneLastFour() throws Exception {
        submit("123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        verifyNoInteractions(rsvpService);
    }

    // 이미 제출했다는 서비스 오류를 충돌 응답으로 전달한다.
    @Test
    @DisplayName("중복 RSVP 제출이면 409와 RSVP_ALREADY_SUBMITTED를 반환한다")
    void returnsConflictForDuplicateResponse() throws Exception {
        doThrow(new BusinessException(RsvpErrorCode.RSVP_ALREADY_SUBMITTED))
                .when(rsvpService).create(eq(PUBLIC_ID), any());

        submit("0123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RSVP_ALREADY_SUBMITTED"));
    }

    // 공개 POST도 CSRF 토큰 없이 제출할 수 없는지 확인한다.
    @Test
    @DisplayName("CSRF 토큰 없이 RSVP를 제출하면 403을 반환한다")
    void rejectsRequestWithoutCsrfToken() throws Exception {
        mockMvc.perform(post(URL, PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("0123")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(rsvpService);
    }

    // 실제 CSRF 초기화 API에서 받은 쿠키와 헤더를 사용해 제출한다.
    private ResultActions submit(String phoneLastFour) throws Exception {
        Cookie csrfCookie = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();

        return mockMvc.perform(post(URL, PUBLIC_ID)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(phoneLastFour)));
    }

    // 전화번호 뒷자리 검증에 사용할 참석 요청 본문을 생성한다.
    private String requestBody(String phoneLastFour) {
        return """
                {
                  "guestName": "김하객",
                  "guestSide": "GROOM",
                  "phoneLastFour": "%s",
                  "attending": true,
                  "companionCount": 2,
                  "mealCount": 1
                }
                """.formatted(phoneLastFour);
    }
}
