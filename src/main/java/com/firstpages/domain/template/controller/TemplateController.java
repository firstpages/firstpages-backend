package com.firstpages.domain.template.controller;

import com.firstpages.domain.template.dto.response.TemplateListResponseDto;
import com.firstpages.domain.template.service.TemplateService;
import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.ResponseFactory;
import com.firstpages.global.response.code.CommonSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 청첩장 템플릿 요청을 처리하는 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/templates")
public class TemplateController {

    // 템플릿 서비스
    private final TemplateService templateService;

    // 선택 가능한 청첩장 템플릿 목록을 반환
    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateListResponseDto>>> getTemplates() {
        List<TemplateListResponseDto> responseDto = templateService.getTemplates();

        return ResponseFactory.success(
                CommonSuccessCode.OK,
                responseDto
        );
    }

}
