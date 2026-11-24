package com.firstpages.domain.invitation.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 업로드한 대표 사진의 S3 객체 키를 반환
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InvitationCoverPhotoUploadResponseDto {

    // 작성본의 대표 사진을 지정할 때 사용하는 객체 키
    private final String storageKey;

    public static InvitationCoverPhotoUploadResponseDto of(String storageKey) {
        return InvitationCoverPhotoUploadResponseDto.builder()
                .storageKey(storageKey)
                .build();
    }
}
