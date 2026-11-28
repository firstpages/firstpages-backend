package com.firstpages.domain.guestbook.controller;

import com.firstpages.domain.guestbook.dto.request.GuestbookCreateRequestDto;
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
import com.firstpages.global.security.oauth2.CustomOAuth2UserService;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationFailureHandler;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.node.JsonNodeFactory;

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
        controllers = {PublicGuestbookController.class, CsrfController.class},
        properties = "app.cors.allowed-origins=http://localhost:5173"
)
@Import({
        SecurityConfig.class, GlobalExceptionHandler.class, SecurityErrorResponseWriter.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class
})
@DisplayName("하객용 방명록 작성 API")
class PublicGuestbookControllerTest {

    // 하객용 메시지 작성 경로
    private static final String URL = "/api/public/invitations/{publicId}/guestbooks";

    // URL에 전달할 공유 식별자
    private static final String PUBLIC_ID = "0123456789abcdef0123456789abcdef";

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

    // 비로그인 상태에서 입력의 최소 길이와 최대 길이를 허용하는지 확인한다.
    @ParameterizedTest(name = "{displayName} (이름 {0}자, 메시지 {1}자)")
    @CsvSource({"1, 1", "20, 500"})
    @DisplayName("하객은 허용된 길이의 이름과 메시지로 로그인 없이 작성할 수 있다")
    void createsMessageWithoutAuthentication(int nameLength, int messageLength) throws Exception {
        String name = "가".repeat(nameLength);
        String message = "축".repeat(messageLength);

        create(name, message)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CREATED"));

        ArgumentCaptor<GuestbookCreateRequestDto> request =
                ArgumentCaptor.forClass(GuestbookCreateRequestDto.class);
        verify(guestbookService).create(eq(PUBLIC_ID), request.capture());
        assertThat(request.getValue().getAuthorName()).isEqualTo(name);
        assertThat(request.getValue().getMessage()).isEqualTo(message);
    }

    // 필수 입력을 누락했거나 공백만 보내면 서비스 호출 전에 거절한다.
    @ParameterizedTest(name = "{displayName} [{index}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("이름이나 메시지가 누락되거나 비어 있으면 400을 반환한다")
    void rejectsMissingOrBlankInput(String invalidInput) throws Exception {
        create(invalidInput, "결혼 축하합니다.")
                .andExpect(status().isBadRequest());
        create("하객", invalidInput)
                .andExpect(status().isBadRequest());
        verifyNoInteractions(guestbookService);
    }

    // 필드별 허용 길이를 한 글자 넘긴 요청을 거절한다.
    @ParameterizedTest(name = "{displayName} (이름 {0}자, 메시지 {1}자)")
    @CsvSource({"21, 1", "1, 501"})
    @DisplayName("이름이나 메시지가 최대 길이를 넘으면 400을 반환한다")
    void rejectsOverlongInput(int nameLength, int messageLength) throws Exception {
        create("가".repeat(nameLength), "축".repeat(messageLength))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        verifyNoInteractions(guestbookService);
    }

    // 공개본을 찾지 못했을 때 기존 청첩장 오류 응답을 반환한다.
    @Test
    @DisplayName("공개 청첩장이 없으면 404와 INVITATION_NOT_FOUND를 반환한다")
    void returnsNotFoundWhenInvitationIsUnavailable() throws Exception {
        doThrow(new BusinessException(InvitationErrorCode.INVITATION_NOT_FOUND))
                .when(guestbookService).create(eq(PUBLIC_ID), any());

        create("하객", "결혼 축하합니다.")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
    }

    // 공개본에서 비활성화된 방명록은 충돌 응답으로 알린다.
    @Test
    @DisplayName("공개본의 방명록이 꺼져 있으면 409와 GUESTBOOK_DISABLED를 반환한다")
    void returnsConflictWhenGuestbookIsDisabled() throws Exception {
        doThrow(new BusinessException(GuestbookErrorCode.GUESTBOOK_DISABLED))
                .when(guestbookService).create(eq(PUBLIC_ID), any());

        create("하객", "결혼 축하합니다.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_DISABLED"));
    }

    // 공개 POST에도 기존 CSRF 검증이 적용되는지 확인한다.
    @Test
    @DisplayName("CSRF 토큰 없이 작성하면 403을 반환한다")
    void rejectsRequestWithoutCsrfToken() throws Exception {
        mockMvc.perform(post(URL, PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("하객", "결혼 축하합니다.")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(guestbookService);
    }

    // 로그인 인증 없이 유효한 CSRF 토큰을 포함한 작성 요청을 전송한다.
    private ResultActions create(String name, String message) throws Exception {
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
                .content(requestBody(name, message)));
    }

    // 누락된 값은 필드를 생략해 실제 JSON 요청을 만든다.
    private String requestBody(String name, String message) {
        var body = JsonNodeFactory.instance.objectNode();
        if (name != null) {
            body.put("authorName", name);
        }
        if (message != null) {
            body.put("message", message);
        }
        return body.toString();
    }
}
