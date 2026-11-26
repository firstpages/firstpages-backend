package com.firstpages.domain.guestbook.exception;

import com.firstpages.global.response.code.ErrorResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GuestbookErrorCode implements ErrorResponseCode {

    // 현재 공개본에서 방명록 작성이 비활성화된 경우
    GUESTBOOK_DISABLED(
            HttpStatus.CONFLICT,
            "GUESTBOOK_DISABLED",
            "이 청첩장은 현재 방명록을 받지 않습니다."
    ),

    // 해당 청첩장에 삭제할 방명록 메시지가 없는 경우
    GUESTBOOK_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "GUESTBOOK_NOT_FOUND",
            "방명록 메시지를 찾을 수 없습니다."
    );

    private final HttpStatus httpStatus;

    private final String code;

    private final String message;
}
