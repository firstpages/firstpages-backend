package com.firstpages.domain.invitation.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.firstpages.domain.invitation.entity.Invitation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 청첩장 작성본 저장 결과 응답 DTO
@JsonPropertyOrder({
        "id",
        "version"
})
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InvitationUpdateResponseDto {

    // 저장한 청첩장 고유 식별자
    private final Long id;

    // 저장으로 확정된 청첩장 버전
    private final Integer version;

    // 조회한 청첩장을 저장 결과 응답으로 변환
    public static InvitationUpdateResponseDto from(
            Invitation invitation
    ) {
        return InvitationUpdateResponseDto.builder()
                .id(invitation.getId())
                .version(invitation.getVersion())
                .build();
    }
}
