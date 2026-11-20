package com.firstpages.domain.invitation.dto.response;

import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 청첩장 생성 결과 응답 DTO
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InvitationCreateResponseDto {

    // 생성된 청첩장 고유 식별자
    private final Long id;

    // 청첩장 공개 상태
    private final InvitationStatus status;

    // 청첩장 엔티티를 생성 응답 DTO로 변환
    public static InvitationCreateResponseDto from(Invitation invitation) {
        return InvitationCreateResponseDto.builder()
                .id(invitation.getId())
                .status(invitation.getStatus())
                .build();
    }
}
