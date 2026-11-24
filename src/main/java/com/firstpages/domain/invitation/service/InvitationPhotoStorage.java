package com.firstpages.domain.invitation.service;

// 청첩장 사진의 저장과 삭제에 필요한 계약을 정의
public interface InvitationPhotoStorage {

    // 지정한 객체 키와 콘텐츠 타입으로 사진을 저장
    void upload(
            String storageKey,
            byte[] imageBytes,
            String contentType
    );

    // 실패한 저장 요청에서 생성한 사진 객체를 삭제
    void delete(
            String storageKey
    );

}
