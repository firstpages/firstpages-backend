package com.firstpages.domain.invitation.mapper;

import com.firstpages.domain.invitation.service.InvitationPhotoUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

// 소유권을 확인한 작성본의 사진에 표시 URL을 추가
@Component
@RequiredArgsConstructor
public class InvitationOwnerPhotoMapper {

    private final InvitationPhotoUrlService photoUrlService;

    // 목록에 표시할 대표 사진만 복사해 조회 URL을 추가
    public JsonNode toCoverPhoto(JsonNode body) {
        JsonNode photo = body.path("coverPhoto").deepCopy();
        addReadUrl(photo);
        return photo;
    }

    // 상세 응답용 본문을 복사해 각 사진에 조회 URL을 추가
    public JsonNode toDetailBody(JsonNode body) {
        JsonNode responseBody = body.deepCopy();

        addReadUrl(responseBody.path("coverPhoto"));
        addReadUrl(responseBody.path("guestbook").path("photo"));

        JsonNode photos = responseBody.path("gallery").path("photos");
        
        if (photos.isArray()) {
            for (JsonNode photo : photos) {
                addReadUrl(photo);
            }
        }

        return responseBody;
    }

    // 저장된 객체 키가 있는 사진에만 서명 URL을 추가
    private void addReadUrl(JsonNode photo) {
        if (!(photo instanceof ObjectNode photoObject)) {
            return;
        }

        JsonNode key = photoObject.path("storageKey");
        if (key.isString() && !key.stringValue().isBlank()) {
            photoObject.put("url", photoUrlService.createReadUrl(key.stringValue()));
        }
    }
}