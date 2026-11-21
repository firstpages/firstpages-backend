package com.firstpages.domain.template.controller;

import com.firstpages.domain.template.dto.response.TemplateDetailResponseDto;
import com.firstpages.domain.template.dto.response.TemplateListResponseDto;
import com.firstpages.domain.template.entity.Template;
import com.firstpages.domain.template.exception.TemplateErrorCode;
import com.firstpages.domain.template.service.TemplateService;
import com.firstpages.global.config.SecurityConfig;
import com.firstpages.global.exception.BusinessException;
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
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

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

    // 비로그인 사용자가 상세 정보와 미리보기 JSON을 받을 수 있는지 검증
    @Test
    @DisplayName("인증 없이 템플릿 상세를 조회하면 200과 미리보기 데이터를 반환한다")
    void getDetailWithoutAuthentication() throws Exception {
        // given
        TemplateDetailResponseDto responseDto = createDetailResponse();

        when(templateService.getDetail(6L))
                .thenReturn(responseDto);

        // when & then
        mockMvc.perform(get(BASE_URL + "/{templateId}", 6L))
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(TemplateController.class))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."))
                .andExpect(jsonPath("$.data.id").value(6))
                .andExpect(jsonPath("$.data.code").value("heart-note"))
                .andExpect(jsonPath("$.data.name").value("하트 노트"))
                .andExpect(jsonPath("$.data.subtitle")
                        .value("종이 위에 붙인 작은 사랑"))
                .andExpect(jsonPath("$.data.description")
                        .value("하트 장식이 있는 청첩장입니다."))
                .andExpect(jsonPath("$.data.tags.length()").value(2))
                .andExpect(jsonPath("$.data.tags[0]").value("손글씨"))
                .andExpect(jsonPath("$.data.tags[1]").value("로맨틱"))
                .andExpect(jsonPath("$.data.coverUrl").value(
                        "/images/template-samples/color-transparent-v1/standing-embrace.png"
                ))
                .andExpect(jsonPath("$.data.defaultFontKey")
                        .value("lee-seoyun"))
                .andExpect(jsonPath("$.data.previewBody").isMap())
                .andExpect(jsonPath("$.data.previewBody.font")
                        .value("lee-seoyun"))
                .andExpect(jsonPath("$.data.previewBody.basic.groomName")
                        .value("박서준"))
                .andExpect(jsonPath("$.data.previewBody.basic.brideName")
                        .value("김다은"))
                .andExpect(jsonPath("$.data.previewBody.venue.name")
                        .value("엘리에나호텔"));

        verify(templateService).getDetail(6L);
        verifyNoInteractions(authenticationEntryPoint);
    }

    // 존재하지 않는 템플릿 예외가 공통 오류 응답으로 전달되는지 검증
    @Test
    @DisplayName("존재하지 않는 템플릿 상세를 조회하면 404를 반환한다")
    void getDetailNotFound() throws Exception {
        // given
        when(templateService.getDetail(999L))
                .thenThrow(new BusinessException(
                        TemplateErrorCode.TEMPLATE_NOT_FOUND
                ));

        // when & then
        mockMvc.perform(get(BASE_URL + "/{templateId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code")
                        .value("TEMPLATE_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("템플릿을 찾을 수 없습니다."));

        verify(templateService).getDetail(999L);
    }

    // 선택 불가능한 템플릿 예외가 공통 오류 응답으로 전달되는지 검증
    @Test
    @DisplayName("선택 불가능한 템플릿 상세를 조회하면 400을 반환한다")
    void getDetailNotSelectable() throws Exception {
        // given
        when(templateService.getDetail(6L))
                .thenThrow(new BusinessException(
                        TemplateErrorCode.TEMPLATE_NOT_SELECTABLE
                ));

        // when & then
        mockMvc.perform(get(BASE_URL + "/{templateId}", 6L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code")
                        .value("TEMPLATE_NOT_SELECTABLE"))
                .andExpect(jsonPath("$.message")
                        .value("선택할 수 없는 템플릿입니다."));

        verify(templateService).getDetail(6L);
    }

    // 숫자가 아닌 경로 변수를 서비스 호출 전에 차단하는지 검증
    @Test
    @DisplayName("템플릿 ID가 숫자가 아니면 400을 반환한다")
    void getDetailWithInvalidId() throws Exception {
        // when & then
        mockMvc.perform(get(BASE_URL + "/invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_PARAMETER"));

        verifyNoInteractions(templateService);
    }

    // 상세 응답의 필드와 JSON 직렬화를 검증할 데이터를 생성
    private TemplateDetailResponseDto createDetailResponse() {
        ObjectNode previewBody = JsonNodeFactory.instance.objectNode();
        previewBody.put("font", "lee-seoyun");

        previewBody.putObject("basic")
                .put("groomName", "박서준")
                .put("brideName", "김다은")
                .put("weddingDate", "2027-05-22")
                .put("weddingTime", "12:30:00");

        previewBody.putObject("venue")
                .put("name", "엘리에나호텔")
                .put("hall", "5층 그랜드볼룸")
                .put("address", "서울특별시 강남구 논현로 645")
                .put("lat", 37.5111132)
                .put("lng", 127.0314126);

        Template template = Template.create(
                "heart-note",
                "하트 노트",
                "종이 위에 붙인 작은 사랑",
                "하트 장식이 있는 청첩장입니다.",
                "handwriting",
                List.of("손글씨", "로맨틱"),
                "/images/template-samples/color-transparent-v1/standing-embrace.png",
                "lee-seoyun",
                previewBody,
                true
        );

        ReflectionTestUtils.setField(template, "id", 6L);

        return TemplateDetailResponseDto.from(template);
    }

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

    // 인증되지 않은 사용자도 템플릿 목록을 조회할 수 있는지 검증
    @Test
    @DisplayName("인증 없이 템플릿 목록을 조회해도 200을 반환한다")
    void getTemplatesWithoutAuthentication() throws Exception {
        // given
        when(templateService.getTemplates())
                .thenReturn(List.of());

        // when & then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(templateService).getTemplates();
        verifyNoInteractions(authenticationEntryPoint);
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
                JsonNodeFactory.instance.objectNode(),
                true
        );

        ReflectionTestUtils.setField(template, "id", 6L);

        return template;
    }
}