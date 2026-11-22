package com.firstpages.domain.invitation.service;

import com.firstpages.domain.template.entity.Template;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

// 새 청첩장의 초기 작성본 JSON을 생성
@Component
public class InvitationDraftBodyFactory {

    // 템플릿의 기본 글꼴과 빈 입력값으로 작성본을 구성
    public JsonNode create(Template template) {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.put("font", template.getDefaultFontKey());

        // 작성 전에는 이름을 비우고 예식 날짜와 시간을 미정으로 둠
        body.putObject("basic")
                .put("groomName", "")
                .put("brideName", "")
                .putNull("weddingDate")
                .putNull("weddingTime");

        // 지도 위치를 확인하기 전에는 좌표를 미정으로 둠
        body.putObject("venue")
                .put("name", "")
                .put("hall", "")
                .put("address", "")
                .putNull("lat")
                .putNull("lng");

        // 사진과 목록은 처음에 비어 있는 상태로 생성
        body.putNull("coverPhoto");
        body.putObject("gallery")
                .putArray("photos");
        body.putObject("parents")
                .putArray("items");
        body.putObject("contacts")
                .putArray("items");
        body.putObject("accounts")
                .putArray("items");
        body.putObject("notices")
                .putArray("items");
        body.putObject("interview")
                .putArray("items");

        // 인사말과 교통 안내는 입력값의 유무로 표시 여부를 판단
        body.putObject("greeting")
                .put("title", "")
                .put("body", "");

        body.putObject("transport")
                .put("transit", "")
                .put("parking", "")
                .put("shuttle", "");

        // RSVP와 방명록은 기본적으로 비활성화
        body.putObject("rsvp")
                .put("enabled", false)
                .put("title", "")
                .put("description", "");

        body.putObject("guestbook")
                .put("enabled", false)
                .putNull("photo");

        // 공유 문구가 비어 있으면 기본 문구를 사용
        body.putObject("share")
                .put("title", "")
                .put("description", "");

        return body;
    }
}