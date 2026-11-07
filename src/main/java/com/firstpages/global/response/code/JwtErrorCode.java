package com.firstpages.global.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum JwtErrorCode implements ErrorResponseCode {

    INVALID_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "INVALID_TOKEN",
            "유효하지 않은 토큰입니다."
    ),

    EXPIRED_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "EXPIRED_TOKEN",
            "만료된 토큰입니다."
    ),

    INVALID_TOKEN_TYPE(
            HttpStatus.UNAUTHORIZED,
            "INVALID_TOKEN_TYPE",
            "올바르지 않은 토큰 유형입니다."
    );

    private final HttpStatus httpStatus;

    private final String code;

    private final String message;
}
