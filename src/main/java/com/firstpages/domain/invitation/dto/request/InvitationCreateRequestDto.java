package com.firstpages.domain.invitation.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 청첩장 생성 요청 DTO
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationCreateRequestDto {

    // 사용자가 선택한 디자인 템플릿 고유 식별자
    @NotNull
    private Long templateId;
}
