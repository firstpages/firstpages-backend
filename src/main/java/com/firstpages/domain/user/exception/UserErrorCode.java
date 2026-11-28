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
    ),

    // 카카오 연결 해제 결과를 확인하지 못한 경우
    KAKAO_UNLINK_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "KAKAO_UNLINK_FAILED",
            "카카오 연결을 해제하지 못했습니다. 잠시 후 다시 시도해 주세요."
    );

    private final HttpStatus httpStatus;

    private final String code;

    private final String message;
}
