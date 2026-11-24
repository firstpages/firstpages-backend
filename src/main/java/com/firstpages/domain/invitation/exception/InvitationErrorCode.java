
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
    ),

    // 다른 요청에서 먼저 수정하여 청첩장 버전이 일치하지 않음
    INVITATION_VERSION_CONFLICT(
            HttpStatus.CONFLICT,
            "INVITATION_VERSION_CONFLICT",
            "다른 요청에서 청첩장이 수정되었습니다. 새로고침 후 다시 시도해 주세요."
    ),

    // 청첩장 작성본의 JSON 구조 또는 필드 값이 올바르지 않음
    INVITATION_INVALID_BODY(
            HttpStatus.BAD_REQUEST,
            "INVITATION_INVALID_BODY",
            "청첩장 작성본 형식이 올바르지 않습니다."
    ),

    // 파일이 비어 있거나 지원하는 이미지 형식과 크기 조건을 만족하지 않을 때 사용한다.
    INVITATION_INVALID_COVER_PHOTO(
            HttpStatus.BAD_REQUEST,
            "INVITATION_INVALID_COVER_PHOTO",
            "대표 사진은 2,400만 픽셀 이하의 JPEG 또는 PNG여야 합니다."
    ),

    // 대표 사진의 파일 크기가 업로드 한도를 초과했을 때 사용한다.
    INVITATION_PHOTO_TOO_LARGE(
            HttpStatus.CONTENT_TOO_LARGE,
            "INVITATION_PHOTO_TOO_LARGE",
            "대표 사진은 5MB 이하여야 합니다."
    ),

    // S3에 대표 사진을 저장하지 못했을 때 사용한다.
    INVITATION_PHOTO_STORAGE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "INVITATION_PHOTO_STORAGE_UNAVAILABLE",
            "대표 사진을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요."
    );

    // HTTP 응답 상태
    private final HttpStatus httpStatus;

    // 서비스 내부 오류 코드
    private final String code;

    // 클라이언트에 제공할 오류 메시지
    private final String message;
}
