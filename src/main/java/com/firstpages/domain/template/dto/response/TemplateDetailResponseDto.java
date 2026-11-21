package com.firstpages.domain.template.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.firstpages.domain.template.entity.Template;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

import java.util.List;

// 청첩장 템플릿의 상세 정보 및 미리보기 응답 DTO
@JsonPropertyOrder({
        "id",
        "code",
        "name",
        "subtitle",
        "description",
        "tags",
        "coverUrl",
        "defaultFontKey",
        "previewBody"
})
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TemplateDetailResponseDto {

    // 템플릿 고유 식별자
    private final Long id;

    // 프론트에서 디자인을 구분하는 템플릿 코드
    private final String code;

    // 템플릿 이름
    private final String name;

    // 템플릿 부제목
    private final String subtitle;

    // 템플릿 상세 설명
    private final String description;

    // 템플릿 태그 목록
    private final List<String> tags;

    // 템플릿 대표 이미지 접근 경로
    private final String coverUrl;

    // 템플릿 기본 글꼴 식별자
    private final String defaultFontKey;

    // 완성된 예시 청첩장의 미리보기 JSON 데이터
    private final JsonNode previewBody;

    // 템플릿 엔티티를 상세 응답 DTO로 변환
    public static TemplateDetailResponseDto from(Template template) {
        return TemplateDetailResponseDto.builder()
                .id(template.getId())
                .code(template.getCode())
                .name(template.getName())
                .subtitle(template.getSubtitle())
                .description(template.getDescription())
                .tags(List.copyOf(template.getTags()))
                .coverUrl(template.getCoverUrl())
                .defaultFontKey(template.getDefaultFontKey())
                .previewBody(template.getPreviewBody())
                .build();
    }
}
