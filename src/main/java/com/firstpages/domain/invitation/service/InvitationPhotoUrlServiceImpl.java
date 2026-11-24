package com.firstpages.domain.invitation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

// 비공개 S3 사진을 일정 시간 읽을 수 있는 URL을 발급
@Service
public class InvitationPhotoUrlServiceImpl implements InvitationPhotoUrlService {

    // 편집 화면에서 사용할 조회 URL의 유효기간
    private static final Duration READ_URL_DURATION = Duration.ofMinutes(15);

    private final S3Presigner s3Presigner;

    private final String bucket;

    public InvitationPhotoUrlServiceImpl(
            S3Presigner s3Presigner,
            @Value("${app.invitation-photos.bucket}") String bucket
    ) {
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    // 지정된 사진 객체에 대한 GET 요청을 서명
    @Override
    public String createReadUrl(String storageKey) {
        return s3Presigner.presignGetObject(request -> request
                        .signatureDuration(READ_URL_DURATION)
                        .getObjectRequest(object -> object
                                .bucket(bucket)
                                .key(storageKey)
                        )
                )
                .url()
                .toExternalForm();
    }
}
