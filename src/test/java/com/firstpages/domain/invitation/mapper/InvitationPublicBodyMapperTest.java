package com.firstpages.domain.invitation.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationPublicBodyMapperTest {

    // 토글 없는 섹션과 사진을 하객용 응답 계약에 맞게 변환하는지 확인
    @Test
    @DisplayName("토글 없이 입력한 내용을 표시하고 사진에는 공개 URL을 사용한다")
    void mapsPublicBody() {
        ObjectNode source = JsonNodeFactory.instance.objectNode();
        source.putObject("greeting").put("title", "결혼식에 초대합니다");
        source.putObject("parents").putArray("items").addObject()
                .put("name", "김영수")
                .put("phone", "");
        source.putObject("contacts").putArray("items").addObject()
                .put("side", "groom")
                .put("phone", "010-0000-0003");
        source.putObject("coverPhoto")
                .put("storageKey", "test/cover.jpg")
                .put("focalX", 50);

        JsonNode result = new InvitationPublicBodyMapper().toPublicBody(
                source,
                key -> "test/cover.jpg".equals(key)
                        ? Optional.of("https://example.com/images/cover.jpg")
                        : Optional.empty()
        );

        assertThat(result.path("greeting").path("title").stringValue())
                .isEqualTo("결혼식에 초대합니다");
        assertThat(result.path("parents").path("items").path(0).has("phone"))
                .isFalse();
        assertThat(result.path("contacts").path("items").path(0).path("phone").stringValue())
                .isEqualTo("010-0000-0003");
        assertThat(result.path("coverPhoto").path("url").stringValue())
                .isEqualTo("https://example.com/images/cover.jpg");
        assertThat(result.path("coverPhoto").path("focalX").intValue()).isEqualTo(50);
        assertThat(result.path("coverPhoto").has("storageKey")).isFalse();
    }
}