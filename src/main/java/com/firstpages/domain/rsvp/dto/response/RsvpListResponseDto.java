package com.firstpages.domain.rsvp.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.firstpages.global.response.PageResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 요청한 응답 페이지와 청첩장 전체의 참석 집계를 반환 응답 DTO
@JsonPropertyOrder({
        "responses",
        "summary"
})
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RsvpListResponseDto {

    // 더보기로 조회한 참석 응답 페이지
    private final PageResponse<RsvpItemResponseDto> responses;

    // 전체 응답의 신랑·신부 측별 집계
    private final Summary summary;

    public static RsvpListResponseDto of(
            PageResponse<RsvpItemResponseDto> responses,
            Summary summary
    ) {
        return RsvpListResponseDto.builder()
                .responses(responses)
                .summary(summary)
                .build();
    }

    // 신랑 측과 신부 측의 집계 결과
    @JsonPropertyOrder({
            "groom",
            "bride"
    })
    @Getter
    @Builder(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Summary {

        // 신랑 측 전체 응답 집계
        private final SideTotals groom;

        // 신부 측 전체 응답 집계
        private final SideTotals bride;

        public static Summary of(
                SideTotals groom,
                SideTotals bride
        ) {
            return Summary.builder()
                    .groom(groom)
                    .bride(bride)
                    .build();
        }
    }

    // 신랑 또는 신부 한쪽의 참석, 불참, 식사 집계
    @JsonPropertyOrder({
            "attendingGuests",
            "declinedResponses",
            "mealGuests"
    })
    @Getter
    @Builder(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SideTotals {

        // 본인과 동반인을 포함한 참석 인원
        private final long attendingGuests;

        // 불참으로 제출한 응답 건수
        private final long declinedResponses;

        // 식사할 전체 인원
        private final long mealGuests;

        public static SideTotals of(
                long attendingGuests,
                long declinedResponses,
                long mealGuests
        ) {
            return SideTotals.builder()
                    .attendingGuests(attendingGuests)
                    .declinedResponses(declinedResponses)
                    .mealGuests(mealGuests)
                    .build();
        }
    }
}
