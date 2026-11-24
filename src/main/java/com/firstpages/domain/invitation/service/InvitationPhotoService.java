package com.firstpages.domain.invitation.service;

import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

// 청첩장 작성본의 사진 처리 기능을 정의
public interface InvitationPhotoService {

    // 기존 사진 키를 검증하고 새 사진 키가 반영된 작성본을 반환
    ObjectNode resolvePhotos(
            Long invitationId,
            JsonNode currentBody,
            ObjectNode requestBody,
            Map<String, List<MultipartFile>> photoParts
    );

}
