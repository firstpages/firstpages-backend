package com.firstpages.domain.rsvp.exception;

import com.firstpages.global.response.code.ErrorResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 참석 응답의 제출 조건에 대한 오류를 정의
@Getter
@RequiredArgsConstructor
public enum RsvpErrorCode implements ErrorResponseCode {

    // 현재 공개본에서 참석 응답을 받지 않는 경우
    RSVP_DISABLED(
            HttpStatus.CONFLICT,
            "RSVP_DISABLED",
            "이 청첩장은 현재 참석 응답을 받지 않습니다."
    ),

    // 같은 하객 정보로 이미 응답을 제출한 경우
    RSVP_ALREADY_SUBMITTED(
            HttpStatus.CONFLICT,
            "RSVP_ALREADY_SUBMITTED",
            "이미 제출된 참석 응답이 있어요."
    ),

    // 참석 여부와 동반·식사 인원의 조합이 올바르지 않은 경우
    RSVP_INVALID_COUNTS(
            HttpStatus.BAD_REQUEST,
            "RSVP_INVALID_COUNTS",
            "참석 여부에 맞게 동반 인원과 식사 인원을 확인해 주세요."
    );

    // 클라이언트에 반환할 HTTP 상태
    private final HttpStatus httpStatus;

    // 오류 종류를 식별하는 코드
    private final String code;

    // 클라이언트에 전달할 오류 메시지
    private final String message;
}
