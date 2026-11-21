package com.firstpages.domain.invitation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

// 청첩장 관리용 제목과 전체 작성본 저장 요청 DTO
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationUpdateRequestDto {

    // 청첩장 관리용 제목으로 빈 문자열은 허용
    @NotNull(message = "청첩장 관리용 제목은 필수입니다.")
    @Size(max = 50, message = "청첩장 관리용 제목은 50자 이하여야 합니다.")
    private String managementTitle;

    // 상세 조회 시 받은 버전으로 이전 작성본의 덮어쓰기를 방지
    @NotNull(message = "청첩장 버전은 필수입니다.")
    @PositiveOrZero(message = "청첩장 버전은 0 이상이어야 합니다.")
    private Integer version;

    // 사용자가 편집한 전체 청첩장 작성본
    @NotNull(message = "청첩장 작성본은 필수입니다.")
    private JsonNode body;
}
