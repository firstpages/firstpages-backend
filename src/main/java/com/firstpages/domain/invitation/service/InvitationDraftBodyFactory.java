package com.firstpages.domain.invitation.service;


import com.firstpages.domain.template.entity.Template;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

// 청첩장 초기 작성본 JSON 생성 컴포넌트
@Component
public class InvitationDraftBodyFactory {

    // 초기 JSON 생성에 사용하는 Jackson 2 매퍼
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 선택한 템플릿을 기준으로 초기 작성본 생성
    public JsonNode create(Template template) {
        ObjectNode body = objectMapper.createObjectNode();

        // 템플릿 기본 글꼴 설정
        body.put("font", template.getDefaultFontKey());

        // 신랑·신부 및 예식 정보
        ObjectNode basic = body.putObject("basic");
        basic.put("groomName", "");
        basic.put("brideName", "");
        basic.putNull("weddingDate");
        basic.putNull("weddingTime");

        // 예식장 정보
        ObjectNode venue = body.putObject("venue");
        venue.put("name", "");
        venue.put("hall", "");
        venue.put("address", "");
        venue.putNull("lat");
        venue.putNull("lng");

        // 대표 사진
        body.putNull("coverPhoto");

        // 인사말
        ObjectNode greeting = body.putObject("greeting");
        greeting.put("enabled", false);
        greeting.put("title", "");
        greeting.put("body", "");

        // 갤러리
        ObjectNode gallery = body.putObject("gallery");
        gallery.put("enabled", false);
        gallery.putArray("photos");

        // 혼주 정보
        ObjectNode parents = body.putObject("parents");
        parents.put("enabled", false);
        parents.putArray("items");

        // 연락처
        ObjectNode contacts = body.putObject("contacts");
        contacts.put("enabled", false);
        contacts.putArray("items");

        // 계좌 정보
        ObjectNode accounts = body.putObject("accounts");
        accounts.put("enabled", false);
        accounts.putArray("items");

        // 교통 안내
        ObjectNode transport = body.putObject("transport");
        transport.put("enabled", false);
        transport.put("transit", "");
        transport.put("parking", "");
        transport.put("shuttle", "");

        // 안내 사항
        ObjectNode notices = body.putObject("notices");
        notices.put("enabled", false);
        notices.putArray("items");

        // 신랑·신부 인터뷰
        ObjectNode interview = body.putObject("interview");
        interview.put("enabled", false);
        interview.put("title", "");
        interview.putArray("items");

        // 참석 여부
        ObjectNode rsvp = body.putObject("rsvp");
        rsvp.put("enabled", false);
        rsvp.put("title", "");
        rsvp.put("description", "");

        // 방명록
        ObjectNode guestbook = body.putObject("guestbook");
        guestbook.put("enabled", false);
        guestbook.putNull("photo");

        // 공유 정보
        ObjectNode share = body.putObject("share");
        share.put("title", "");
        share.put("description", "");

        return body;
    }
}
