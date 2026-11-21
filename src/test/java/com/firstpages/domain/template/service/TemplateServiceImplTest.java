package com.firstpages.domain.template.service;

import com.firstpages.domain.template.dto.response.TemplateDetailResponseDto;
import com.firstpages.domain.template.dto.response.TemplateListResponseDto;
import com.firstpages.domain.template.entity.Template;
import com.firstpages.domain.template.exception.TemplateErrorCode;
import com.firstpages.domain.template.repository.TemplateRepository;
import com.firstpages.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// 템플릿 목록 조회 서비스의 응답 변환과 빈 목록 처리를 검증
@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private TemplateRepository templateRepository;

    @InjectMocks
    private TemplateServiceImpl templateService;

    // 템플릿 상세 정보와 미리보기 JSON이 응답으로 변환되는지 검증
    @Test
    @DisplayName("선택 가능한 템플릿의 상세 정보와 미리보기 데이터를 반환한다")
    void getDetailSuccess() {
        // given
        Template template = createPreviewTemplate(true);

        when(templateRepository.findById(6L))
                .thenReturn(Optional.of(template));

        // when
        TemplateDetailResponseDto result = templateService.getDetail(6L);

        // then
        assertAll(
                () -> assertEquals(6L, result.getId()),
                () -> assertEquals("heart-note", result.getCode()),
                () -> assertEquals("하트 노트", result.getName()),
                () -> assertEquals(
                        "종이 위에 붙인 작은 사랑",
                        result.getSubtitle()
                ),
                () -> assertEquals(
                        "하트 장식이 있는 청첩장입니다.",
                        result.getDescription()
                ),
                () -> assertEquals(
                        List.of("손글씨", "로맨틱"),
                        result.getTags()
                ),
                () -> assertEquals(
                        "/images/template-samples/color-transparent-v1/standing-embrace.png",
                        result.getCoverUrl()
                ),
                () -> assertEquals("lee-seoyun", result.getDefaultFontKey()),
                () -> assertEquals(
                        template.getPreviewBody(),
                        result.getPreviewBody()
                ),
                () -> assertEquals(
                        "박서준",
                        result.getPreviewBody()
                                .path("basic")
                                .path("groomName")
                                .stringValue()
                ),
                () -> assertEquals(
                        "김다은",
                        result.getPreviewBody()
                                .path("basic")
                                .path("brideName")
                                .stringValue()
                )
        );

        verify(templateRepository).findById(6L);
        verifyNoMoreInteractions(templateRepository);
    }

    // 존재하지 않는 템플릿을 조회하면 도메인 예외가 발생하는지 검증
    @Test
    @DisplayName("존재하지 않는 템플릿을 상세 조회하면 예외가 발생한다")
    void getDetailNotFound() {
        // given
        when(templateRepository.findById(999L))
                .thenReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> templateService.getDetail(999L)
        );

        // then
        assertEquals(
                TemplateErrorCode.TEMPLATE_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(templateRepository).findById(999L);
        verifyNoMoreInteractions(templateRepository);
    }

    // 선택 불가능한 템플릿의 상세 조회를 차단하는지 검증
    @Test
    @DisplayName("선택 불가능한 템플릿을 상세 조회하면 예외가 발생한다")
    void getDetailNotSelectable() {
        // given
        Template template = createPreviewTemplate(false);

        when(templateRepository.findById(6L))
                .thenReturn(Optional.of(template));

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> templateService.getDetail(6L)
        );

        // then
        assertEquals(
                TemplateErrorCode.TEMPLATE_NOT_SELECTABLE,
                exception.getErrorCode()
        );

        verify(templateRepository).findById(6L);
        verifyNoMoreInteractions(templateRepository);
    }

    // 상세 응답 변환을 검증할 미리보기 데이터와 템플릿을 생성
    private Template createPreviewTemplate(boolean selectable) {
        ObjectNode previewBody = JsonNodeFactory.instance.objectNode();
        previewBody.put("font", "lee-seoyun");

        previewBody.putObject("basic")
                .put("groomName", "박서준")
                .put("brideName", "김다은")
                .put("weddingDate", "2027-05-22")
                .put("weddingTime", "12:30:00");

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
                selectable
        );

        ReflectionTestUtils.setField(template, "id", 6L);

        return template;
    }

    // 조회한 템플릿의 모든 응답 필드와 목록 순서가 유지되는지 검증
    @Test
    @DisplayName("조회한 템플릿을 순서대로 목록 응답 DTO로 변환한다")
    void getTemplatesSuccess() {
        // given
        Template firstTemplate = createTemplate(
                6L,
                "heart-note",
                "하트 노트",
                "종이 위에 붙인 작은 사랑",
                List.of("손글씨", "로맨틱"),
                "/images/template-samples/color-transparent-v1/standing-embrace.png"
        );

        Template secondTemplate = createTemplate(
                7L,
                "paper-cover",
                "페이퍼 커버",
                "두 사람의 첫 페이지",
                List.of("흑백", "일러스트"),
                "/images/templates/paper-cover/thumbnail.webp"
        );

        when(templateRepository.findAllBySelectableTrueOrderByIdAsc())
                .thenReturn(List.of(firstTemplate, secondTemplate));

        // when
        List<TemplateListResponseDto> result =
                templateService.getTemplates();

        // then
        assertEquals(2, result.size());

        TemplateListResponseDto firstResponse = result.get(0);
        TemplateListResponseDto secondResponse = result.get(1);

        assertAll(
                () -> assertEquals(6L, firstResponse.getId()),
                () -> assertEquals("heart-note", firstResponse.getCode()),
                () -> assertEquals("하트 노트", firstResponse.getName()),
                () -> assertEquals(
                        "종이 위에 붙인 작은 사랑",
                        firstResponse.getSubtitle()
                ),
                () -> assertEquals(
                        List.of("손글씨", "로맨틱"),
                        firstResponse.getTags()
                ),
                () -> assertEquals(
                        "/images/template-samples/color-transparent-v1/standing-embrace.png",
                        firstResponse.getCoverUrl()
                ),
                () -> assertEquals(7L, secondResponse.getId()),
                () -> assertEquals("paper-cover", secondResponse.getCode()),
                () -> assertEquals("페이퍼 커버", secondResponse.getName()),
                () -> assertEquals(
                        "두 사람의 첫 페이지",
                        secondResponse.getSubtitle()
                ),
                () -> assertEquals(
                        List.of("흑백", "일러스트"),
                        secondResponse.getTags()
                ),
                () -> assertEquals(
                        "/images/templates/paper-cover/thumbnail.webp",
                        secondResponse.getCoverUrl()
                )
        );

        verify(templateRepository)
                .findAllBySelectableTrueOrderByIdAsc();
        verifyNoMoreInteractions(templateRepository);
    }

    // 조회 결과가 없으면 예외 없이 빈 리스트를 반환하는지 검증
    @Test
    @DisplayName("선택 가능한 템플릿이 없으면 빈 리스트를 반환한다")
    void getTemplatesEmpty() {
        // given
        when(templateRepository.findAllBySelectableTrueOrderByIdAsc())
                .thenReturn(List.of());

        // when
        List<TemplateListResponseDto> result =
                templateService.getTemplates();

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(templateRepository)
                .findAllBySelectableTrueOrderByIdAsc();
        verifyNoMoreInteractions(templateRepository);
    }

    // 목록 응답 변환에 사용할 템플릿 엔티티를 생성
    private Template createTemplate(
            Long id,
            String code,
            String name,
            String subtitle,
            List<String> tags,
            String coverUrl
    ) {
        Template template = Template.create(
                code,
                name,
                subtitle,
                "테스트용 템플릿 설명",
                "illustration",
                tags,
                coverUrl,
                "gowun-batang",
                JsonNodeFactory.instance.objectNode(),
                true
        );

        ReflectionTestUtils.setField(template, "id", id);

        return template;
    }
}