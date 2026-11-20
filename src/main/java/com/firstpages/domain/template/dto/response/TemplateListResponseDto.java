package com.firstpages.domain.template.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.firstpages.domain.template.entity.Template;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 선택 가능한 청첩장 템플릿의 목록 응답 DTO
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({
        "id",
        "code",
        "name",
        "subtitle",
        "tags",
        "coverUrl"
})
public class TemplateListResponseDto {

    // 템플릿 고유 식별자
    private final Long id;

    // 프론트에서 디자인을 구분하는 템플릿 코드
    private final String code;

    // 템플릿 이름
    private final String name;

    // 템플릿 부제목
    private final String subtitle;

    // 템플릿 태그 목록
    private final List<String> tags;

    // 템플릿 대표 이미지 접근 경로
    private final String coverUrl;

    // 템플릿 엔티티를 목록 응답 DTO로 변환
    public static TemplateListResponseDto from(Template template) {
        return TemplateListResponseDto.builder()
                .id(template.getId())
                .code(template.getCode())
                .name(template.getName())
                .subtitle(template.getSubtitle())
                .tags(template.getTags())
                .coverUrl(template.getCoverUrl())
                .build();
    }
}
