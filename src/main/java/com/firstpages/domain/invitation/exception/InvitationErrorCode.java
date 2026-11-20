
package com.firstpages.domain.invitation.exception;

import com.firstpages.global.response.code.ErrorResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 청첩장 도메인 예외 코드
@Getter
@RequiredArgsConstructor
public enum InvitationErrorCode implements ErrorResponseCode {

    // 요청한 청첩장이 존재하지 않음
    INVITATION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "INVITATION_NOT_FOUND",
            "청첩장을 찾을 수 없습니다."
    ),

    // 청첩장 접근 권한이 없음
    INVITATION_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "INVITATION_ACCESS_DENIED",
            "해당 청첩장에 접근할 권한이 없습니다."
    ),

    // 청첩장 작성본이 존재하지 않음
    INVITATION_DRAFT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "INVITATION_DRAFT_NOT_FOUND",
            "청첩장 작성본을 찾을 수 없습니다."
    );

    // HTTP 응답 상태
    private final HttpStatus httpStatus;

    // 서비스 내부 오류 코드
    private final String code;

    // 클라이언트에 제공할 오류 메시지
    private final String message;
}
