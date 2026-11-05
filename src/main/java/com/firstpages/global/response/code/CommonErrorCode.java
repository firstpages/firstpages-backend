package com.firstpages.global.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorResponseCode {

    // 400 Bad Request
    INVALID_INPUT(
            HttpStatus.BAD_REQUEST,
            "INVALID_INPUT",
            "입력값이 올바르지 않습니다."
    ),

    INVALID_REQUEST_BODY(
            HttpStatus.BAD_REQUEST,
            "INVALID_REQUEST_BODY",
            "요청 본문이 올바르지 않습니다."
    ),

    INVALID_PARAMETER(
            HttpStatus.BAD_REQUEST,
            "INVALID_PARAMETER",
            "요청 파라미터가 올바르지 않습니다."
    ),

    MISSING_PARAMETER(
            HttpStatus.BAD_REQUEST,
            "MISSING_PARAMETER",
            "필수 요청 파라미터가 누락되었습니다."
    ),

    // 401 Unauthorized
    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            "인증이 필요합니다."
    ),

    // 403 Forbidden
    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "ACCESS_DENIED",
            "접근 권한이 없습니다."
    ),

    // 404 Not Found
    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "요청한 리소스를 찾을 수 없습니다."
    ),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            "지원하지 않는 HTTP 메서드입니다."
    ),

    // 415 Unsupported Media Type
    UNSUPPORTED_MEDIA_TYPE(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "UNSUPPORTED_MEDIA_TYPE",
            "지원하지 않는 Content-Type입니다."
    ),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다."
    );

    private final HttpStatus httpStatus;

    private final String code;

    private final String message;
}
