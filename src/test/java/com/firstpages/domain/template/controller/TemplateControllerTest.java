package com.firstpages.domain.template.controller;

import com.firstpages.domain.template.dto.response.TemplateListResponseDto;
import com.firstpages.domain.template.entity.Template;
import com.firstpages.domain.template.service.TemplateService;
import com.firstpages.global.config.SecurityConfig;
import com.firstpages.global.security.SecurityErrorResponseWriter;
import com.firstpages.global.security.jwt.JwtAccessDeniedHandler;
import com.firstpages.global.security.jwt.JwtAuthenticationEntryPoint;
import com.firstpages.global.security.jwt.JwtProvider;
import com.firstpages.global.security.oauth2.CustomOAuth2UserService;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationFailureHandler;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 템플릿 목록 API의 응답 구조와 인증 처리를 검증
@WebMvcTest(
        controllers = TemplateController.class,
        properties = "app.cors.allowed-origins=http://localhost:5173"
)
@Import(SecurityConfig.class)
class TemplateControllerTest {

    private static final String BASE_URL = "/api/templates";

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TemplateService templateService;

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

    // 템플릿 목록이 합의한 응답 필드와 공통 응답 구조로 반환되는지 검증
    @Test
    @DisplayName("템플릿 목록 조회에 성공하면 200과 템플릿 배열을 반환한다")
    void getTemplatesSuccess() throws Exception {
        // given
        TemplateListResponseDto responseDto =
                TemplateListResponseDto.from(createTemplate());

        when(templateService.getTemplates())
                .thenReturn(List.of(responseDto));

        // when & then
        mockMvc.perform(
                        get(BASE_URL)
                                .with(authenticatedUser())
                )
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(TemplateController.class))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(6))
                .andExpect(jsonPath("$.data[0].code").value("heart-note"))
                .andExpect(jsonPath("$.data[0].name").value("하트 노트"))
                .andExpect(jsonPath("$.data[0].subtitle")
                        .value("종이 위에 붙인 작은 사랑"))
                .andExpect(jsonPath("$.data[0].tags.length()").value(2))
                .andExpect(jsonPath("$.data[0].tags[0]").value("손글씨"))
                .andExpect(jsonPath("$.data[0].tags[1]").value("로맨틱"))
                .andExpect(jsonPath("$.data[0].coverUrl").value(
                        "/images/template-samples/color-transparent-v1/standing-embrace.png"
                ))
                .andExpect(jsonPath("$.data[0].coverImageUrl").doesNotExist())
                .andExpect(jsonPath("$.data[0].description").doesNotExist())
                .andExpect(jsonPath("$.data[0].category").doesNotExist());

        verify(templateService).getTemplates();
    }

    // 조회 결과가 없으면 null 대신 빈 JSON 배열을 반환하는지 검증
    @Test
    @DisplayName("선택 가능한 템플릿이 없으면 200과 빈 배열을 반환한다")
    void getTemplatesEmpty() throws Exception {
        // given
        when(templateService.getTemplates())
                .thenReturn(List.of());

        // when & then
        mockMvc.perform(
                        get(BASE_URL)
                                .with(authenticatedUser())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(templateService).getTemplates();
    }

    // 인증되지 않은 요청은 보안 필터에서 차단하는지 검증
    @Test
    @DisplayName("인증 없이 템플릿 목록을 조회하면 401을 반환한다")
    void getTemplatesWithoutAuthentication() throws Exception {
        // given
        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }).when(authenticationEntryPoint).commence(any(), any(), any());

        // when & then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(templateService);
        verify(authenticationEntryPoint).commence(any(), any(), any());
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

    // 응답 JSON 검증에 사용할 템플릿 엔티티를 생성
    private Template createTemplate() {
        Template template = Template.create(
                "heart-note",
                "하트 노트",
                "종이 위에 붙인 작은 사랑",
                "테스트용 템플릿 설명",
                "illustration",
                List.of("손글씨", "로맨틱"),
                "/images/template-samples/color-transparent-v1/standing-embrace.png",
                "gowun-batang",
                true
        );

        ReflectionTestUtils.setField(template, "id", 6L);

        return template;
    }
}