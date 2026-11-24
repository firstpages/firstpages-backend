package com.firstpages.domain.invitation.mapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

// 공개 청첩장 사진의 저장 키를 CloudFront 조회 URL로 변환
@Component
public class InvitationPublicPhotoUrlResolver {

    // 끝에 슬래시가 없는 CloudFront 배포 주소
    private final String baseUrl;

    public InvitationPublicPhotoUrlResolver(
            @Value("${app.invitation-photos.cloudfront-base-url}") String baseUrl
    ) {
        this.baseUrl = baseUrl.replaceFirst("/+$", "");
    }

    // 청첩장 사진 키가 있으면 하객용 조회 URL을 반환
    public Optional<String> resolve(String storageKey) {
        return Optional.ofNullable(storageKey)
                .filter(key -> !key.isBlank())
                .map(key -> baseUrl + "/" + key);
    }
}