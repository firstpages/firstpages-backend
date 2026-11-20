package com.firstpages.domain.invitation.controller;


import com.firstpages.domain.invitation.dto.request.InvitationCreateRequestDto;
import com.firstpages.domain.invitation.dto.response.InvitationCreateResponseDto;
import com.firstpages.domain.invitation.dto.response.InvitationDetailResponseDto;
import com.firstpages.domain.invitation.dto.response.InvitationListResponseDto;
import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationStatus;
import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.domain.invitation.service.InvitationService;
import com.firstpages.domain.template.entity.Template;
import com.firstpages.domain.template.exception.TemplateErrorCode;
import com.firstpages.global.config.SecurityConfig;
import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.security.SecurityErrorResponseWriter;
import com.firstpages.global.security.jwt.JwtAccessDeniedHandler;
import com.firstpages.global.security.jwt.JwtAuthenticationEntryPoint;
import com.firstpages.global.security.jwt.JwtProvider;
import com.firstpages.global.security.oauth2.CustomOAuth2UserService;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationFailureHandler;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 청첩장 API의 요청 검증과 응답 및 보안 필터 동작을 검증
@WebMvcTest(
        controllers = InvitationController.class,
        properties = "app.cors.allowed-origins=http://localhost:5173"
)
@Import(SecurityConfig.class)
class InvitationControllerTest {

    private static final String BASE_URL = "/api/invitations";

    private static final Long USER_ID = 1L;
    private static final Long TEMPLATE_ID = 10L;
    private static final Long INVITATION_ID = 301L;

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String CSRF_TOKEN = "test-csrf-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationService invitationService;

    // 실제 보안 설정을 구성하는 JWT 관련 의존성
    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SecurityErrorResponseWriter responseWriter;

    @MockitoBean
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    // 실제 보안 설정을 구성하는 OAuth2 관련 의존성
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler successHandler;

    @MockitoBean
    private OAuth2AuthenticationFailureHandler failureHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    // Mock 보안 예외 처리기가 인증 실패와 접근 거부 상태를 반환하도록 설정
    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }).when(authenticationEntryPoint).commence(any(), any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }).when(accessDeniedHandler).handle(any(), any(), any());
    }

    // 생성 요청을 서비스에 전달하고 생성 결과를 반환하는지 검증
    @Test
    @DisplayName("청첩장 생성에 성공하면 201과 생성된 ID 및 상태를 반환한다")
    void createSuccess() throws Exception {
        InvitationDocument document = createDraftDocument();

        InvitationCreateResponseDto responseDto =
                InvitationCreateResponseDto.from(document.getInvitation());

        when(invitationService.create(
                eq(USER_ID),
                any(InvitationCreateRequestDto.class)
        )).thenReturn(responseDto);

        mockMvc.perform(
                        post(BASE_URL)
                                .with(authenticatedUser())
                                .cookie(csrfCookie())
                                .header(CSRF_HEADER_NAME, CSRF_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "templateId": 10
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.message")
                        .value("리소스가 생성되었습니다."))
                .andExpect(jsonPath("$.data.id").value(INVITATION_ID))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.version").doesNotExist());

        ArgumentCaptor<InvitationCreateRequestDto> captor =
                ArgumentCaptor.forClass(InvitationCreateRequestDto.class);

        verify(invitationService).create(eq(USER_ID), captor.capture());

        assertEquals(TEMPLATE_ID, captor.getValue().getTemplateId());
    }

    // 필수 요청 필드가 없으면 서비스 호출 전에 차단하는지 검증
    @Test
    @DisplayName("템플릿 ID 없이 청첩장 생성을 요청하면 400을 반환한다")
    void createWithoutTemplateId() throws Exception {
        mockMvc.perform(
                        post(BASE_URL)
                                .with(authenticatedUser())
                                .cookie(csrfCookie())
                                .header(CSRF_HEADER_NAME, CSRF_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        verifyNoInteractions(invitationService);
    }

    // 파싱할 수 없는 JSON 요청을 공통 오류 응답으로 변환하는지 검증
    @Test
    @DisplayName("잘못된 JSON으로 청첩장 생성을 요청하면 400을 반환한다")
    void createWithMalformedJson() throws Exception {
        mockMvc.perform(
                        post(BASE_URL)
                                .with(authenticatedUser())
                                .cookie(csrfCookie())
                                .header(CSRF_HEADER_NAME, CSRF_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"templateId\":")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST_BODY"));

        verifyNoInteractions(invitationService);
    }

    // 서비스의 템플릿 예외가 공통 오류 응답으로 전달되는지 검증
    @Test
    @DisplayName("선택할 수 없는 템플릿으로 생성하면 400을 반환한다")
    void createWithUnselectableTemplate() throws Exception {
        when(invitationService.create(
                eq(USER_ID),
                any(InvitationCreateRequestDto.class)
        )).thenThrow(
                new BusinessException(
                        TemplateErrorCode.TEMPLATE_NOT_SELECTABLE
                )
        );

        mockMvc.perform(
                        post(BASE_URL)
                                .with(authenticatedUser())
                                .cookie(csrfCookie())
                                .header(CSRF_HEADER_NAME, CSRF_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "templateId": 10
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code")
                        .value("TEMPLATE_NOT_SELECTABLE"))
                .andExpect(jsonPath("$.message")
                        .value("선택할 수 없는 템플릿입니다."));

        verify(invitationService).create(
                eq(USER_ID),
                any(InvitationCreateRequestDto.class)
        );
    }

    // 로그인 사용자의 청첩장 목록을 배열로 반환하는지 검증
    @Test
    @DisplayName("내 청첩장 목록을 조회하면 200과 청첩장 배열을 반환한다")
    void getInvitationsSuccess() throws Exception {
        InvitationListResponseDto responseDto =
                InvitationListResponseDto.from(createDraftDocument());

        when(invitationService.getInvitations(USER_ID))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(
                        get(BASE_URL)
                                .with(authenticatedUser())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(INVITATION_ID))
                .andExpect(jsonPath("$.data[0].managementTitle").value(""))
                .andExpect(jsonPath("$.data[0].templateName")
                        .value("페이퍼 커버"))
                .andExpect(jsonPath("$.data[0].status").value("DRAFT"));

        verify(invitationService).getInvitations(USER_ID);
    }

    // 청첩장이 없는 사용자에게 빈 배열을 반환하는지 검증
    @Test
    @DisplayName("내 청첩장이 없으면 200과 빈 배열을 반환한다")
    void getInvitationsEmpty() throws Exception {
        when(invitationService.getInvitations(USER_ID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get(BASE_URL)
                                .with(authenticatedUser())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(invitationService).getInvitations(USER_ID);
    }

    // 사용자 ID와 청첩장 ID를 서비스에 전달하고 작성본을 반환하는지 검증
    @Test
    @DisplayName("내 청첩장 상세를 조회하면 200과 작성본 JSON을 반환한다")
    void getDetailSuccess() throws Exception {
        InvitationDetailResponseDto responseDto =
                InvitationDetailResponseDto.from(createDraftDocument());

        when(invitationService.getDetail(USER_ID, INVITATION_ID))
                .thenReturn(responseDto);

        mockMvc.perform(
                        get(BASE_URL + "/{invitationId}", INVITATION_ID)
                                .with(authenticatedUser())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(INVITATION_ID))
                .andExpect(jsonPath("$.data.templateId").value(TEMPLATE_ID))
                .andExpect(jsonPath("$.data.templateCode")
                        .value("paper-cover"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.body.font")
                        .value("gowun-batang"))
                .andExpect(jsonPath("$.data.body.basic.groomName")
                        .value(""));

        verify(invitationService).getDetail(USER_ID, INVITATION_ID);
    }

    // 조회할 수 없는 청첩장에 대한 서비스 예외를 응답으로 변환하는지 검증
    @Test
    @DisplayName("조회할 수 없는 청첩장 상세를 요청하면 404를 반환한다")
    void getDetailNotFound() throws Exception {
        when(invitationService.getDetail(USER_ID, INVITATION_ID))
                .thenThrow(
                        new BusinessException(
                                InvitationErrorCode.INVITATION_NOT_FOUND
                        )
                );

        mockMvc.perform(
                        get(BASE_URL + "/{invitationId}", INVITATION_ID)
                                .with(authenticatedUser())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code")
                        .value("INVITATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("청첩장을 찾을 수 없습니다."));

        verify(invitationService).getDetail(USER_ID, INVITATION_ID);
    }

    // 숫자가 아닌 경로 변수를 서비스 호출 전에 차단하는지 검증
    @Test
    @DisplayName("청첩장 ID가 숫자가 아니면 400을 반환한다")
    void getDetailWithInvalidId() throws Exception {
        mockMvc.perform(
                        get(BASE_URL + "/invalid-id")
                                .with(authenticatedUser())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        verifyNoInteractions(invitationService);
    }

    // 인증되지 않은 사용자의 요청을 보안 필터에서 차단하는지 검증
    @Test
    @DisplayName("인증 없이 내 청첩장 목록을 조회하면 401을 반환한다")
    void getInvitationsWithoutAuthentication() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(invitationService);
        verify(authenticationEntryPoint).commence(any(), any(), any());
    }

    // 인증되어도 CSRF 토큰이 없는 생성 요청을 차단하는지 검증
    @Test
    @DisplayName("CSRF 토큰 없이 청첩장 생성을 요청하면 403을 반환한다")
    void createWithoutCsrfToken() throws Exception {
        mockMvc.perform(
                        post(BASE_URL)
                                .with(authenticatedUser())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "templateId": 10
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(invitationService);
    }

    // 실제 인증 principal과 동일한 Long 사용자 ID를 요청에 설정
    private RequestPostProcessor authenticatedUser() {
        return authentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        USER_ID,
                        null,
                        List.of()
                )
        );
    }

    // SPA 방식의 CSRF 검증에 사용할 쿠키를 생성
    private Cookie csrfCookie() {
        Cookie cookie = new Cookie(CSRF_COOKIE_NAME, CSRF_TOKEN);
        cookie.setPath("/");
        return cookie;
    }

    // 응답 DTO 변환에 필요한 엔티티와 초기 작성본 데이터를 구성
    private InvitationDocument createDraftDocument() {
        Invitation invitation = mock(Invitation.class);
        Template template = mock(Template.class);
        InvitationDocument document = mock(InvitationDocument.class);

        when(invitation.getId()).thenReturn(INVITATION_ID);
        when(invitation.getManagementTitle()).thenReturn("");
        when(invitation.getStatus()).thenReturn(InvitationStatus.DRAFT);

        when(template.getId()).thenReturn(TEMPLATE_ID);
        when(template.getCode()).thenReturn("paper-cover");
        when(template.getName()).thenReturn("페이퍼 커버");

        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.put("font", "gowun-batang");
        body.putObject("basic")
                .put("groomName", "")
                .put("brideName", "")
                .putNull("weddingDate")
                .putNull("weddingTime");
        body.putNull("coverPhoto");

        when(document.getInvitation()).thenReturn(invitation);
        when(document.getTemplate()).thenReturn(template);
        when(document.getBody()).thenReturn(body);

        return document;
    }
}