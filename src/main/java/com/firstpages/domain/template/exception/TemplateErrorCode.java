
package com.firstpages.domain.template.exception;

import com.firstpages.global.response.code.ErrorResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 템플릿 도메인 예외 코드
@Getter
@RequiredArgsConstructor
public enum TemplateErrorCode implements ErrorResponseCode {

    // 요청한 템플릿이 존재하지 않음
    TEMPLATE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "TEMPLATE_NOT_FOUND",
            "템플릿을 찾을 수 없습니다."
    ),

    // 현재 선택할 수 없는 템플릿
    TEMPLATE_NOT_SELECTABLE(
            HttpStatus.BAD_REQUEST,
            "TEMPLATE_NOT_SELECTABLE",
            "선택할 수 없는 템플릿입니다."
    );

    // HTTP 응답 상태
    private final HttpStatus httpStatus;

    // 서비스 내부 오류 코드
    private final String code;

    // 클라이언트에 제공할 오류 메시지
    private final String message;
}
