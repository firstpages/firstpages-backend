package com.firstpages.domain.invitation.service;

// 청첩장 사진의 표시 URL을 발급하는 계약
public interface InvitationPhotoUrlService {

    // 소유권이 확인된 사진의 객체 키로 조회 URL을 발급
    String createReadUrl(
            String storageKey
    );

}
