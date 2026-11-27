package com.firstpages.domain.rsvp.controller;

import com.firstpages.domain.rsvp.dto.response.RsvpItemResponseDto;
import com.firstpages.domain.rsvp.dto.response.RsvpListResponseDto;
import com.firstpages.domain.rsvp.entity.Rsvp;
import com.firstpages.domain.rsvp.entity.RsvpGuestSide;
import com.firstpages.global.response.PageResponse;
import com.firstpages.domain.rsvp.service.RsvpService;
import com.firstpages.global.config.SecurityConfig;
import com.firstpages.global.exception.GlobalExceptionHandler;
import com.firstpages.global.security.CsrfController;
import com.firstpages.global.security.SecurityErrorResponseWriter;
import com.firstpages.global.security.jwt.JwtAccessDeniedHandler;
import com.firstpages.global.security.jwt.JwtAuthenticationEntryPoint;
import com.firstpages.global.security.jwt.JwtProvider;
import com.firstpages.global.security.jwt.JwtTokenType;
import com.firstpages.global.security.oauth2.CustomOAuth2UserService;
import com.firstpages.global.security.oauth2.OAuth2CookieManager;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationFailureHandler;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// RSVP 목록 요청의 인증과 응답 형식을 검증한다.
@WebMvcTest(
        controllers = {RsvpController.class, CsrfController.class},
        properties = "app.cors.allowed-origins=http://localhost:5173"
)
@Import({
        SecurityConfig.class, GlobalExceptionHandler.class, SecurityErrorResponseWriter.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class
})
@DisplayName("소유자용 RSVP 관리 API")
class RsvpControllerTest {

    // 소유자용 RSVP 목록 경로
    private static final String URL = "/api/invitations/{invitationId}/rsvps";

    // URL에 전달할 청첩장 내부 식별자
    private static final Long INVITATION_ID = 42L;

    // 인증된 요청에 적용할 사용자 식별자
    private static final Long USER_ID = 7L;

    // JWT 공급자가 검증하도록 전달할 테스트용 토큰
    private static final String ACCESS_TOKEN = "test-access-token";

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

    // 인증된 사용자의 페이지 번호를 서비스에 전달하고 목록과 양측 집계를 반환한다.
    @Test
    @DisplayName("소유자의 더보기 요청에 응답 페이지와 신랑·신부 측 통계를 반환한다")
    void returnsPageAndSummary() throws Exception {
        Cookie cookie = authenticate();
        Page<RsvpItemResponseDto> page = Page.empty(PageRequest.of(1, 20));
        var summary = RsvpListResponseDto.Summary.of(
                RsvpListResponseDto.SideTotals.of(5, 2, 4),
                RsvpListResponseDto.SideTotals.of(0, 0, 0)
        );
        when(rsvpService.getRsvps(USER_ID, INVITATION_ID, 1, 20))
                .thenReturn(RsvpListResponseDto.of(PageResponse.from(page), summary));

        mockMvc.perform(get(URL, INVITATION_ID).cookie(cookie).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.responses.page").value(1))
                .andExpect(jsonPath("$.data.responses.size").value(20))
                .andExpect(jsonPath("$.data.responses.items").isEmpty())
                .andExpect(jsonPath("$.data.summary.groom.attendingGuests").value(5))
                .andExpect(jsonPath("$.data.summary.groom.declinedResponses").value(2))
                .andExpect(jsonPath("$.data.summary.groom.mealGuests").value(4))
                .andExpect(jsonPath("$.data.summary.bride.attendingGuests").value(0));

        verify(rsvpService).getRsvps(USER_ID, INVITATION_ID, 1, 20);
    }

    // 로그인하지 않은 요청은 서비스 호출 전에 차단한다.
    @Test
    @DisplayName("비로그인 사용자의 RSVP 목록 요청은 401을 반환한다")
    void rejectsAnonymousListing() throws Exception {
        mockMvc.perform(get(URL, INVITATION_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(rsvpService);
    }

    // 실제 PUT 경로에서 편집 요청을 받고 수정된 항목을 반환한다.
    @Test
    @DisplayName("소유자의 PUT 요청은 수정된 RSVP 항목을 반환한다")
    void updatesResponse() throws Exception {
        Cookie auth = authenticate();
        Cookie token = csrfCookie();
        Rsvp rsvp = Rsvp.create(null, "이하객", RsvpGuestSide.BRIDE, "0123", false, 0, 0);
        when(rsvpService.update(eq(USER_ID), eq(INVITATION_ID), eq(21L), any()))
                .thenReturn(RsvpItemResponseDto.from(rsvp));

        mockMvc.perform(put(URL + "/{rsvpId}", INVITATION_ID, 21L)
                        .cookie(auth, token)
                        .header("X-XSRF-TOKEN", token.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"guestName":"이하객","guestSide":"BRIDE","attending":false,
                                 "companionCount":0,"mealCount":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.guestName").value("이하객"))
                .andExpect(jsonPath("$.data.guestSide").value("BRIDE"))
                .andExpect(jsonPath("$.data.attending").value(false))
                .andExpect(jsonPath("$.data.phoneLastFour").doesNotHaveJsonPath());

        verify(rsvpService).update(eq(USER_ID), eq(INVITATION_ID), eq(21L), any());
    }

    // 삭제 경로에서 숫자 RSVP ID를 서비스에 전달하는지 확인한다.
    @Test
    @DisplayName("소유자의 DELETE 요청에서 경로의 RSVP ID를 전달하고 200을 반환한다")
    void deletesResponse() throws Exception {
        Cookie auth = authenticate();
        Cookie token = csrfCookie();

        mockMvc.perform(delete(URL + "/{rsvpId}", INVITATION_ID, 21L)
                        .cookie(auth, token)
                        .header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(rsvpService).delete(USER_ID, INVITATION_ID, 21L);
    }

    // 실제 CSRF 초기화 API에서 사용할 토큰 쿠키를 발급받는다.
    private Cookie csrfCookie() throws Exception {
        Cookie token = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        assertThat(token).isNotNull();
        return token;
    }

    // 테스트용 인증 쿠키를 서비스에서 사용할 사용자 ID에 연결한다.
    private Cookie authenticate() {
        when(jwtProvider.getUserId(ACCESS_TOKEN, JwtTokenType.ACCESS)).thenReturn(USER_ID);
        return new Cookie(OAuth2CookieManager.ACCESS_TOKEN, ACCESS_TOKEN);
    }
}
