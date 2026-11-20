package com.firstpages.domain.template.service;

import com.firstpages.domain.template.dto.response.TemplateListResponseDto;
import com.firstpages.domain.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 청첩장 템플릿 서비스 구현체
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    // 템플릿 Repository
    private final TemplateRepository templateRepository;

    // 선택 가능한 템플릿을 조회하고 목록 응답 DTO로 변환
    @Override
    public List<TemplateListResponseDto> getTemplates() {
        return templateRepository.findAllBySelectableTrueOrderByIdAsc()
                .stream()
                .map(TemplateListResponseDto::from)
                .toList();
    }
}
