package com.firstpages.domain.guestbook.controller;

import com.firstpages.domain.guestbook.dto.response.GuestbookItemResponseDto;
import com.firstpages.domain.guestbook.exception.GuestbookErrorCode;
import com.firstpages.domain.guestbook.service.GuestbookService;
import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.domain.user.repository.UserRepository;
import com.firstpages.global.config.SecurityConfig;
import com.firstpages.global.exception.BusinessException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 방명록 관리 요청의 인증, 목록 응답과 삭제 응답을 검증한다.
@WebMvcTest(
        controllers = {GuestbookController.class, CsrfController.class},
        properties = "app.cors.allowed-origins=http://localhost:5173"
)
@Import({
        SecurityConfig.class, GlobalExceptionHandler.class, SecurityErrorResponseWriter.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class
})
@DisplayName("소유자용 방명록 관리 API")
class GuestbookControllerTest {

    // 소유자용 방명록 목록 경로
    private static final String URL = "/api/invitations/{invitationId}/guestbooks";

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
    private GuestbookService guestbookService;

    // JWT 보안 필터의 의존성을 제공한다.
    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserRepository userRepository;

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

    // 관리 화면의 더보기 요청에 다음 페이지와 전체 페이지 수를 반환한다.
    @Test
    @DisplayName("로그인한 소유자의 더보기 요청에 요청한 페이지와 전체 페이지 수를 반환한다")
    void returnsRequestedPage() throws Exception {
        Cookie cookie = authenticate();
        PageRequest request = PageRequest.of(1, 20);
        Page<GuestbookItemResponseDto> page = new PageImpl<>(
                Collections.emptyList(), request, 45
        );
        when(guestbookService.getGuestbooks(USER_ID, INVITATION_ID, 1, 20)).thenReturn(page);

        mockMvc.perform(get(URL, INVITATION_ID).cookie(cookie).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.items").isArray());

        verify(guestbookService).getGuestbooks(USER_ID, INVITATION_ID, 1, 20);
    }

    // 메시지가 없는 첫 페이지도 빈 목록을 포함한 성공 응답으로 반환한다.
    @Test
    @DisplayName("메시지가 없으면 기본 첫 페이지에 빈 목록을 반환한다")
    void returnsEmptyFirstPage() throws Exception {
        Cookie cookie = authenticate();
        when(guestbookService.getGuestbooks(USER_ID, INVITATION_ID, 0, 20))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        mockMvc.perform(get(URL, INVITATION_ID).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    // 소유권 확인 실패를 기존 404 오류 응답으로 전달한다.
    @Test
    @DisplayName("소유권이 확인되지 않으면 404를 반환한다")
    void returnsNotFoundForNonOwner() throws Exception {
        Cookie cookie = authenticate();
        when(guestbookService.getGuestbooks(USER_ID, INVITATION_ID, 0, 20))
                .thenThrow(new BusinessException(InvitationErrorCode.INVITATION_NOT_FOUND));

        mockMvc.perform(get(URL, INVITATION_ID).cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
    }

    // 인증하지 않은 요청은 서비스에 도달하기 전에 거절한다.
    @Test
    @DisplayName("로그인하지 않은 사용자의 방명록 목록 요청은 401을 반환한다")
    void rejectsAnonymousListing() throws Exception {
        mockMvc.perform(get(URL, INVITATION_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(guestbookService);
    }

    // 삭제 요청의 인증 정보와 두 경로 변수를 서비스에 전달한다.
    @Test
    @DisplayName("로그인한 소유자가 메시지를 삭제하면 200을 반환한다")
    void deletesGuestbook() throws Exception {
        Cookie authCookie = authenticate();
        Cookie csrfCookie = csrfCookie();

        mockMvc.perform(delete(URL + "/{guestbookId}", INVITATION_ID, 21L)
                        .cookie(authCookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(guestbookService).delete(USER_ID, INVITATION_ID, 21L);
    }

    // 대상 청첩장에 메시지가 없으면 방명록 오류 코드를 전달한다.
    @Test
    @DisplayName("삭제할 메시지가 해당 청첩장에 없으면 404를 반환한다")
    void returnsNotFoundForMissingGuestbook() throws Exception {
        Cookie authCookie = authenticate();
        Cookie csrfCookie = csrfCookie();
        doThrow(new BusinessException(GuestbookErrorCode.GUESTBOOK_NOT_FOUND))
                .when(guestbookService).delete(USER_ID, INVITATION_ID, 21L);

        mockMvc.perform(delete(URL + "/{guestbookId}", INVITATION_ID, 21L)
                        .cookie(authCookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_NOT_FOUND"));
    }

    // 유효한 CSRF 토큰을 가진 비로그인 요청도 인증 단계에서 차단한다.
    @Test
    @DisplayName("로그인하지 않은 사용자가 메시지를 삭제하면 401을 반환한다")
    void rejectsAnonymousDeletion() throws Exception {
        Cookie csrfCookie = csrfCookie();

        mockMvc.perform(delete(URL + "/{guestbookId}", INVITATION_ID, 21L)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(guestbookService);
    }

    // 실제 CSRF 초기화 API에서 삭제 요청에 사용할 쿠키를 가져온다.
    private Cookie csrfCookie() throws Exception {
        Cookie cookie = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    // 실제 보안 필터가 테스트 토큰을 사용자 ID로 해석하도록 설정한다.
    private Cookie authenticate() {
        when(jwtProvider.getUserId(ACCESS_TOKEN, JwtTokenType.ACCESS)).thenReturn(USER_ID);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        return new Cookie(OAuth2CookieManager.ACCESS_TOKEN, ACCESS_TOKEN);
    }
}
