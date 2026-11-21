package com.firstpages.domain.invitation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 저장된 청첩장 작성본의 공개 요청
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationPublishRequestDto {

    // 클라이언트가 조회하거나 저장한 청첩장의 버전
    @NotNull(message = "청첩장 버전은 필수입니다.")
    @PositiveOrZero(message = "청첩장 버전은 0 이상이어야 합니다.")
    private Integer version;
}
