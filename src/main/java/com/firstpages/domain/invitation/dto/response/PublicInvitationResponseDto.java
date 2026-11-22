package com.firstpages.domain.invitation.dto.response;

import com.firstpages.domain.invitation.entity.InvitationDocument;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

// 하객 화면에 필요한 공개 청첩장 정보를 반환
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PublicInvitationResponseDto {

    // 공유 링크에 사용하는 청첩장 식별자
    private final String publicId;

    // 프론트에서 렌더링할 디자인을 선택하는 템플릿 코드
    private final String templateCode;

    // 하객에게 공개할 항목만 담은 본문
    private final JsonNode body;

    // 공개본의 식별 정보와 공개 범위를 적용한 본문으로 응답 생성
    public static PublicInvitationResponseDto of(
            InvitationDocument publishedDocument,
            JsonNode publicBody
    ) {
        return PublicInvitationResponseDto.builder()
                .publicId(publishedDocument.getInvitation().getPublicId())
                .templateCode(publishedDocument.getTemplate().getCode())
                .body(publicBody)
                .build();
    }
}
