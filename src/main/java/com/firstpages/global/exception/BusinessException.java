package com.firstpages.global.exception;

import com.firstpages.global.response.code.ErrorResponseCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorResponseCode errorCode;

    // Enum에 정의된 기본 메시지를 사용합니다.
    public BusinessException(
            ErrorResponseCode errorCode
    ) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 커스텀 메시지를 사용합니다.
    public BusinessException(
            ErrorResponseCode errorCode,
            String message
    ) {
        super(message);
        this.errorCode = errorCode;
    }
}
