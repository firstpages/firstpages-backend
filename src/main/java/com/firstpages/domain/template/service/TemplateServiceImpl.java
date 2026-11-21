package com.firstpages.domain.template.service;

import com.firstpages.domain.template.dto.response.TemplateDetailResponseDto;
import com.firstpages.domain.template.dto.response.TemplateListResponseDto;
import com.firstpages.domain.template.entity.Template;
import com.firstpages.domain.template.exception.TemplateErrorCode;
import com.firstpages.domain.template.repository.TemplateRepository;
import com.firstpages.global.exception.BusinessException;
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

    // 템플릿 존재 여부와 선택 가능 여부를 확인하고 상세 정보를 반환
    @Override
    public TemplateDetailResponseDto getDetail(Long templateId) {
        // 템플릿 조회
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(
                        TemplateErrorCode.TEMPLATE_NOT_FOUND
                ));

        // 템플릿 선택 가능 여부 판단
        if (!template.isSelectable()) {
            throw new BusinessException(
                    TemplateErrorCode.TEMPLATE_NOT_SELECTABLE
            );
        }

        return TemplateDetailResponseDto.from(template);
    }
}
