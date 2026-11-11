package com.firstpages.domain.auth.exception;

import com.firstpages.global.response.code.ErrorResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 인증 도메인의 오류 코드를 정의
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorResponseCode {
    REFRESH_TOKEN_NOT_FOUND(
            HttpStatus.UNAUTHORIZED,
            "REFRESH_TOKEN_NOT_FOUND",
            "리프레시 토큰이 존재하지 않습니다."
    ),

    INVALID_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "INVALID_REFRESH_TOKEN",
            "유효하지 않거나 이미 사용된 리프레시 토큰입니다."
    );

    private final HttpStatus httpStatus;

    private final String code;

    private final String message;
}
