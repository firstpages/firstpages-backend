package com.firstpages.domain.rsvp.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.firstpages.domain.rsvp.entity.Rsvp;
import com.firstpages.domain.rsvp.entity.RsvpGuestSide;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 소유자 관리 목록에 표시할 참석 응답 DTO
@JsonPropertyOrder({
        "id",
        "guestName",
        "guestSide",
        "attending",
        "companionCount",
        "mealCount",
        "createdAt"
})
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RsvpItemResponseDto {

    // 개별 응답을 식별하는 ID
    private final Long id;

    // 하객이 입력한 이름
    private final String guestName;

    // 신랑 측 또는 신부 측 하객 구분
    private final RsvpGuestSide guestSide;

    // 하객 본인의 참석 여부
    private final boolean attending;

    // 본인을 제외한 동반 참석 인원
    private final int companionCount;

    // 본인과 동반인 중 식사할 인원
    private final int mealCount;

    // 참석 응답을 제출한 일시
    private final LocalDateTime createdAt;

    // RSVP 엔티티를 관리 목록의 응답 항목으로 변환
    public static RsvpItemResponseDto from(Rsvp rsvp) {
        return RsvpItemResponseDto.builder()
                .id(rsvp.getId())
                .guestName(rsvp.getGuestName())
                .guestSide(rsvp.getGuestSide())
                .attending(rsvp.isAttending())
                .companionCount(rsvp.getCompanionCount())
                .mealCount(rsvp.getMealCount())
                .createdAt(rsvp.getCreatedAt())
                .build();
    }
}
