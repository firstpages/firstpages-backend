package com.firstpages.global.response.code;

import org.springframework.http.HttpStatus;

public interface ResponseCode {

    // HTTP 상태 코드 반환
    HttpStatus getHttpStatus();

    // 서비스 응답 코드 반환
    String getCode();

    // 기본 응답 메시지 반환
    String getMessage();
}
