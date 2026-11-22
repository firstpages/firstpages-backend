package com.firstpages.domain.invitation.mapper;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Optional;
import java.util.function.Function;

// 저장된 공개본을 하객에게 전달할 JSON으로 변환
@Component
public class InvitationPublicBodyMapper {
    // 공개본에서 하객 화면에 필요한 필드만 골라 새 본문을 생성
    public ObjectNode toPublicBody(
            JsonNode source,
            Function<String, Optional<String>> resolvePhotoUrl
    ) {
        ObjectNode body = copyFields(source, "font");

        // 기본 예식 정보와 화면에 표시할 문구를 복사
        body.set("basic", copyFields(
                source.path("basic"), "groomName", "brideName", "weddingDate", "weddingTime"
        ));
        body.set("venue", copyFields(
                source.path("venue"), "name", "hall", "address", "lat", "lng"
        ));
        body.set("greeting", copyFields(source.path("greeting"), "title", "body"));
        body.set("transport", copyFields(
                source.path("transport"), "transit", "parking", "shuttle"
        ));
        body.set("share", copyFields(source.path("share"), "title", "description"));

        // 대표 사진이 없거나 표시 URL을 확인할 수 없으면 null 반환
        body.putNull("coverPhoto");
        toPublicPhoto(source.path("coverPhoto"), resolvePhotoUrl)
                .ifPresent(photo -> body.set("coverPhoto", photo));

        // 표시 가능한 갤러리 사진을 작성본의 순서대로 추가
        ArrayNode photos = body.putObject("gallery").putArray("photos");

        JsonNode galleryPhotos = source.path("gallery").path("photos");

        if (galleryPhotos.isArray()) {
            for (JsonNode photo : galleryPhotos) {
                toPublicPhoto(photo, resolvePhotoUrl).ifPresent(photos::add);
            }
        }

        // 혼주와 신랑·신부 연락처를 각각의 공개 규칙에 맞춰 변환
        body.set("parents", toPublicParents(source.path("parents")));
        body.set("contacts", toPublicContacts(source.path("contacts")));

        // 계좌·안내·Q&A 목록에서 표시용 필드만 복사
        body.set("accounts", copyItems(
                source.path("accounts"), "id", "side", "relation", "bank", "number", "holder"
        ));
        body.set("notices", copyItems(source.path("notices"), "id", "title", "body"));
        body.set("interview", copyItems(
                source.path("interview"), "id", "question", "groomAnswer", "brideAnswer"
        ));

        // RSVP와 방명록에만 활성 상태를 적용
        body.set("rsvp", toPublicRsvp(source.path("rsvp")));
        body.set("guestbook", toPublicGuestbook(source.path("guestbook"), resolvePhotoUrl));

        return body;
    }

    // 혼주는 이름과 관계를 표시하고 번호가 있을 때만 phone 필드를 추가
    private ObjectNode toPublicParents(JsonNode source) {
        ObjectNode section = JsonNodeFactory.instance.objectNode();
        ArrayNode items = section.putArray("items");
        JsonNode sourceItems = source.path("items");

        if (sourceItems.isArray()) {
            for (JsonNode parent : sourceItems) {
                if (parent.isObject()) {
                    ObjectNode item = copyFields(parent, "id", "side", "relation", "name");
                    JsonNode phone = parent.path("phone");

                    if (hasText(phone)) {
                        item.set("phone", phone);
                    }

                    items.add(item);
                }
            }
        }

        return section;
    }

    // 전화번호가 입력된 신랑·신부 연락처만 포함
    private ObjectNode toPublicContacts(JsonNode source) {
        ObjectNode section = JsonNodeFactory.instance.objectNode();
        ArrayNode items = section.putArray("items");
        JsonNode sourceItems = source.path("items");

        if (sourceItems.isArray()) {
            for (JsonNode contact : sourceItems) {
                if (hasText(contact.path("phone"))) {
                    items.add(copyFields(contact, "id", "side", "phone"));
                }
            }
        }

        return section;
    }

    // RSVP를 끈 경우 제목과 안내 문구를 빈 문자열로 반환
    private ObjectNode toPublicRsvp(JsonNode source) {
        boolean enabled = isEnabled(source);
        ObjectNode section = enabled
                ? copyFields(source, "title", "description")
                : JsonNodeFactory.instance.objectNode().put("title", "").put("description", "");

        return section.put("enabled", enabled);
    }

    // 방명록을 켠 경우에만 사진의 표시 URL을 조회
    private ObjectNode toPublicGuestbook(
            JsonNode source,
            Function<String, Optional<String>> resolvePhotoUrl
    ) {
        boolean enabled = isEnabled(source);
        ObjectNode section = JsonNodeFactory.instance.objectNode();
        section.put("enabled", enabled);
        section.putNull("photo");

        if (enabled) {
            toPublicPhoto(source.path("photo"), resolvePhotoUrl)
                    .ifPresent(photo -> section.set("photo", photo));
        }

        return section;
    }

    // 사진 저장 키로 표시 URL을 조회하고 자르기 정보와 함께 반환
    private Optional<ObjectNode> toPublicPhoto(
            JsonNode source,
            Function<String, Optional<String>> resolvePhotoUrl
    ) {
        JsonNode storageKey = source.path("storageKey");

        if (!hasText(storageKey)) {
            return Optional.empty();
        }

        return resolvePhotoUrl.apply(storageKey.stringValue())
                .map(url -> copyFields(source, "id", "focalX", "focalY", "zoom")
                        .put("url", url));
    }

    // 목록의 순서를 유지하면서 지정한 필드만 복사
    private ObjectNode copyItems(JsonNode source, String... fields) {
        ObjectNode section = JsonNodeFactory.instance.objectNode();
        ArrayNode items = section.putArray("items");
        JsonNode sourceItems = source.path("items");

        if (sourceItems.isArray()) {
            for (JsonNode item : sourceItems) {
                if (item.isObject()) {
                    items.add(copyFields(item, fields));
                }
            }
        }

        return section;
    }

    // 지정한 필드 중 문자열·숫자·불리언·null 같은 단일 값만 복사
    private ObjectNode copyFields(JsonNode source, String... fields) {
        ObjectNode target = JsonNodeFactory.instance.objectNode();

        for (String field : fields) {
            JsonNode value = source.path(field);

            if (value.isValueNode()) {
                target.set(field, value);
            }
        }

        return target;
    }

    // 공백으로만 이루어진 값을 제외하고 문자열 입력 여부를 확인
    private boolean hasText(JsonNode value) {
        return value.isTextual() && !value.stringValue().isBlank();
    }

    // JSON 불리언 true인 경우에만 활성 상태로 처리
    private boolean isEnabled(JsonNode section) {
        JsonNode enabled = section.path("enabled");
        return enabled.isBoolean() && enabled.booleanValue();
    }
}
