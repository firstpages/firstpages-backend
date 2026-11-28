package com.firstpages.domain.invitation.controller;

import com.firstpages.domain.invitation.dto.response.PublicInvitationResponseDto;
import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.domain.invitation.service.InvitationService;
import com.firstpages.domain.template.entity.Template;
import com.firstpages.domain.user.repository.UserRepository;
import com.firstpages.global.config.SecurityConfig;
import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.exception.GlobalExceptionHandler;
import com.firstpages.global.security.SecurityErrorResponseWriter;
import com.firstpages.global.security.jwt.JwtAccessDeniedHandler;
import com.firstpages.global.security.jwt.JwtAuthenticationEntryPoint;
import com.firstpages.global.security.jwt.JwtProvider;
import com.firstpages.global.security.oauth2.CustomOAuth2UserService;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationFailureHandler;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 하객용 공개 조회의 비로그인 접근과 HTTP 응답을 검증
@WebMvcTest(
        controllers = PublicInvitationController.class,
        properties = "app.cors.allowed-origins=http://localhost:5173"
)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PublicInvitationControllerTest {

    private static final String URL = "/api/public/invitations/{publicId}";
    private static final String PUBLIC_ID = "0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    // 공개 조회 서비스의 응답과 예외를 설정
    @MockitoBean
    private InvitationService invitationService;

    // 실제 보안 필터 체인을 구성하는 JWT 의존성
    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SecurityErrorResponseWriter responseWriter;

    @MockitoBean
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    // 실제 보안 필터 체인을 구성하는 OAuth2 의존성
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler successHandler;

    @MockitoBean
    private OAuth2AuthenticationFailureHandler failureHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    // 인증 쿠키 없이 공개 청첩장과 화면에 필요한 정보를 조회
    @Test
    @DisplayName("하객은 로그인 없이 공개 청첩장을 조회할 수 있다")
    void getPublicInvitationWithoutAuthentication() throws Exception {
        PublicInvitationResponseDto response = createPublicResponse();
        when(invitationService.getPublicInvitation(PUBLIC_ID)).thenReturn(response);

        mockMvc.perform(get(URL, PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.publicId").value(PUBLIC_ID))
                .andExpect(jsonPath("$.data.templateCode").value("paper-cover"))
                .andExpect(jsonPath("$.data.body.basic.groomName").value("김지훈"))
                .andExpect(jsonPath("$.data.managementTitle").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.data.version").doesNotHaveJsonPath());

        verify(invitationService).getPublicInvitation(PUBLIC_ID);
        verifyNoInteractions(authenticationEntryPoint);
    }

    // 공개본을 찾지 못하면 비로그인 요청에도 공통 404 응답을 반환
    @Test
    @DisplayName("공개 청첩장을 조회할 수 없으면 404를 반환한다")
    void getPublicInvitationNotFound() throws Exception {
        when(invitationService.getPublicInvitation(PUBLIC_ID))
                .thenThrow(new BusinessException(InvitationErrorCode.INVITATION_NOT_FOUND));

        mockMvc.perform(get(URL, PUBLIC_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));

        verify(invitationService).getPublicInvitation(PUBLIC_ID);
        verifyNoInteractions(authenticationEntryPoint);
    }

    // 공개본의 식별 정보와 가공된 본문으로 테스트 응답을 생성
    private PublicInvitationResponseDto createPublicResponse() {
        Invitation invitation = mock(Invitation.class);
        Template template = mock(Template.class);
        InvitationDocument document = mock(InvitationDocument.class);

        when(invitation.getPublicId()).thenReturn(PUBLIC_ID);
        when(template.getCode()).thenReturn("paper-cover");
        when(document.getInvitation()).thenReturn(invitation);
        when(document.getTemplate()).thenReturn(template);

        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.putObject("basic").put("groomName", "김지훈");

        return PublicInvitationResponseDto.of(document, body);
    }
}