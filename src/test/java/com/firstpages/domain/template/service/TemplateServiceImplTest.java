package com.firstpages.domain.template.service;

import com.firstpages.domain.template.dto.response.TemplateListResponseDto;
import com.firstpages.domain.template.entity.Template;
import com.firstpages.domain.template.repository.TemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// 템플릿 목록 조회 서비스의 응답 변환과 빈 목록 처리를 검증
@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private TemplateRepository templateRepository;

    @InjectMocks
    private TemplateServiceImpl templateService;

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
                true
        );

        ReflectionTestUtils.setField(template, "id", id);

        return template;
    }
}