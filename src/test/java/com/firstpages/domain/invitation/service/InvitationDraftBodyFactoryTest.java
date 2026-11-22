package com.firstpages.domain.invitation.service;

import com.firstpages.domain.template.entity.Template;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// 새 청첩장의 초기 작성본 JSON 구조를 검증
class InvitationDraftBodyFactoryTest {

    // 여기에 createsInitialBodyWithNewContract() 메서드를 추가
    // 실제 팩토리가 새 작성본 계약에 맞는 기본값을 생성하는지 검증
    @Test
    @DisplayName("초기 작성본은 기본 글꼴을 사용하고 RSVP와 방명록에만 토글을 둔다")
    void createsInitialBodyWithNewContract() {
        Template template = mock(Template.class);
        when(template.getDefaultFontKey()).thenReturn("lee-seoyun");

        JsonNode body = new InvitationDraftBodyFactory().create(template);

        assertThat(body.path("font").stringValue()).isEqualTo("lee-seoyun");
        assertThat(body.path("basic").path("weddingDate").isNull()).isTrue();
        assertThat(body.path("venue").path("lat").isNull()).isTrue();
        assertThat(body.path("coverPhoto").isNull()).isTrue();
        assertThat(body.path("gallery").path("photos").isArray()).isTrue();
        assertThat(body.path("gallery").path("photos").size()).isZero();

        for (String section : List.of(
                "greeting", "gallery", "parents", "contacts",
                "accounts", "transport", "notices", "interview"
        )) {
            assertThat(body.path(section).has("enabled")).as(section).isFalse();
        }

        assertThat(body.path("rsvp").path("enabled").isBoolean()).isTrue();
        assertThat(body.path("rsvp").path("enabled").booleanValue()).isFalse();
        assertThat(body.path("guestbook").path("enabled").isBoolean()).isTrue();
        assertThat(body.path("guestbook").path("enabled").booleanValue()).isFalse();
    }
}