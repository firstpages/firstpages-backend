package com.firstpages.domain.invitation.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 청첩장 공개 후 조회한 결과
@JsonPropertyOrder({
        "id",
        "status",
        "publicId",
        "version"
})
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InvitationPublishResponseDto {

    // 청첩장 고유 식별자
    private final Long id;

    // 조회 시점의 공개 상태
    private final InvitationStatus status;

    // 하객용 공유 링크에 사용하는 식별자
    private final String publicId;

    // 조회 시점의 청첩장 버전
    private final Integer version;

    // 청첩장 엔티티를 공개 결과 응답으로 변환
    public static InvitationPublishResponseDto from(Invitation invitation) {
        return InvitationPublishResponseDto.builder()
                .id(invitation.getId())
                .status(invitation.getStatus())
                .publicId(invitation.getPublicId())
                .version(invitation.getVersion())
                .build();
    }
}
