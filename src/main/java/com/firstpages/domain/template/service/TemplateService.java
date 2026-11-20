package com.firstpages.domain.template.service;

import com.firstpages.domain.template.dto.response.TemplateListResponseDto;

import java.util.List;

// 청첩장 템플릿 비즈니스 로직 인터페이스
public interface TemplateService {

    // 사용자가 선택할 수 있는 청첩장 템플릿 목록을 조회
    List<TemplateListResponseDto> getTemplates();
}
