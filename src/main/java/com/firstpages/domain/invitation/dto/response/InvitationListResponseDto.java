package com.firstpages.domain.invitation.dto.response;

import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// 내 청첩장 목록의 개별 항목 응답 DTO
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InvitationListResponseDto {

    // 청첩장 고유 식별자
    private final Long id;

    // 청첩장 관리용 제목
    private final String managementTitle;

    // 적용된 템플릿 이름
    private final String templateName;

    // 청첩장 공개 상태
    private final InvitationStatus status;

    // 예식 날짜
    private final LocalDate weddingDate;

    // 청첩장 대표 사진 정보
    private final CoverPhotoResponseDto coverPhoto;


    // 목록 화면의 대표 사진 응답 DTO
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CoverPhotoResponseDto {
        // 대표 사진 접근 URL
        private final String url;

        // 이미지 가로 초점 위치
        private final Integer focalX;

        // 이미지 세로 초점 위치
        private final Integer focalY;

        // 이미지 확대 비율
        private final Double zoom;

        // 대표 사진 JSON을 목록 표시용 응답으로 변환
        public static CoverPhotoResponseDto from(JsonNode photoNode) {
            if (photoNode == null || !photoNode.isObject()) {
                return null;
            }

            JsonNode urlNode = photoNode.path("url");

            if (!urlNode.isString() || urlNode.stringValue().isBlank()) {
                return null;
            }

            JsonNode focalXNode = photoNode.path("focalX");
            JsonNode focalYNode = photoNode.path("focalY");
            JsonNode zoomNode = photoNode.path("zoom");

            return CoverPhotoResponseDto.builder()
                    .url(urlNode.stringValue())
                    .focalX(focalXNode.isIntegralNumber() && focalXNode.canConvertToInt()
                            ? focalXNode.intValue() : null
                    )
                    .focalY(focalYNode.isIntegralNumber() && focalYNode.canConvertToInt()
                            ? focalYNode.intValue() : null)
                    .zoom(zoomNode.isNumber()
                            ? zoomNode.doubleValue()
                            : null
                    )
                    .build();
        }
    }

    // 청첩장 작성본과 추출한 표시 정보를 목록 응답으로 변환
    public static InvitationListResponseDto from(
            InvitationDocument document
    ) {

        Invitation invitation = document.getInvitation();
        JsonNode body = document.getBody();

        return InvitationListResponseDto.builder()
                .id(invitation.getId())
                .managementTitle(invitation.getManagementTitle())
                .templateName(document.getTemplate().getName())
                .status(invitation.getStatus())
                .weddingDate(readWeddingDate(body
                        .path("basic")
                        .path("weddingDate")
                ))
                .coverPhoto(CoverPhotoResponseDto.from(
                        body.path("coverPhoto")
                ))
                .build();
    }

    // 예식일이 비어 있거나 날짜 형식이 잘못되면 null로 변환
    private static LocalDate readWeddingDate(JsonNode dateNode) {
        if (!dateNode.isString() || dateNode.stringValue().isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(dateNode.stringValue());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }


}
