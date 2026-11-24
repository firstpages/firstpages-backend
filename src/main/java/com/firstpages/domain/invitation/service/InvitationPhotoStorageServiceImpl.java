package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


// 청첩장 사진의 저장과 삭제를 S3에서 처리
@Service
public class InvitationPhotoStorageServiceImpl implements InvitationPhotoStorage {

    // S3에 객체를 업로드하는 AWS SDK 클라이언트
    private final S3Client s3Client;

    // 청첩장 사진을 저장할 버킷의 전체 이름
    private final String bucket;

    public InvitationPhotoStorageServiceImpl(
            S3Client s3Client,
            @Value("${app.invitation-photos.bucket}") String bucket
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    // 검증된 사진을 지정한 S3 객체 키로 업로드
    @Override
    public void upload(
            String storageKey,
            byte[] imageBytes,
            String contentType
    ) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(imageBytes)
            );
        } catch (SdkException e) {
            throw new BusinessException(
                    InvitationErrorCode.INVITATION_PHOTO_STORAGE_UNAVAILABLE
            );
        }
    }

    // DB에서 롤백된 요청의 사진 객체를 삭제
    @Override
    public void delete(
            String storageKey
    ) {
        s3Client.deleteObject(request -> request
                .bucket(bucket)
                .key(storageKey)
        );
    }

}
