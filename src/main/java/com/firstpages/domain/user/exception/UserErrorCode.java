package com.firstpages.domain.user.exception;

import com.firstpages.global.response.code.ErrorResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 사용자 도메인에서 발생하는 오류 코드를 정의
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorResponseCode {

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER_NOT_FOUND",
            "사용자를 찾을 수 없습니다."
    );

    private final HttpStatus httpStatus;

    private final String code;

    private final String message;
}
