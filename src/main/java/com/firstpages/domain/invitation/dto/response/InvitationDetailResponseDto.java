package com.firstpages.domain.invitation.dto.response;


import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationStatus;
import com.firstpages.domain.template.entity.Template;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

// 청첩장 상세 조회 응답 DTO
@Getter
@Builder(access =  AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InvitationDetailResponseDto {

    // 청첩장 고유 식별자
    private final Long id;

    // 청첩장 관리용 제목
    private final String managementTitle;

    // 적용된 템플릿 고유 식별자
    private final Long templateId;

    // 적용된 템플릿 코드
    private final String templateCode;

    // 적용된 템플릿 이름
    private final String templateName;

    // 청첩장 공개 상태
    private final InvitationStatus status;

    // 청첩장 공유 식별자
    private final String publicId;

    // 낙관적 락 버전
    private final Integer version;

    // 청첩장 생성 일시
    private final LocalDateTime createdAt;

    // 청첩장 최종 수정 일시
    private final LocalDateTime updatedAt;

    // 청첩장 작성본 JSON 데이터
    private final JsonNode body;

    // 청첩장 및 작성본 데이터를 조합해 상세 응답 DTO 생성
    public static InvitationDetailResponseDto from(
            InvitationDocument document
    ) {
        Invitation invitation = document.getInvitation();
        Template template = document.getTemplate();

        return InvitationDetailResponseDto.builder()
                .id(invitation.getId())
                .managementTitle(invitation.getManagementTitle())
                .templateId(template.getId())
                .templateCode(template.getCode())
                .templateName(template.getName())
                .status(invitation.getStatus())
                .publicId(invitation.getPublicId())
                .version(invitation.getVersion())
                .createdAt(invitation.getCreatedAt())
                .updatedAt(invitation.getUpdatedAt())
                .body(document.getBody())
                .build();
    }
}
