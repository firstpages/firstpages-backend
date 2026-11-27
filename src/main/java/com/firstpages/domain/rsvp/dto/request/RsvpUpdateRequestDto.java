package com.firstpages.domain.rsvp.dto.request;

import com.firstpages.domain.rsvp.entity.RsvpGuestSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 소유자가 편집 화면에서 저장하는 RSVP 응답 DTO
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RsvpUpdateRequestDto {

    // 수정할 하객 이름
    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 20, message = "이름은 최대 20자입니다.")
    private String guestName;

    // 수정할 신랑 측 또는 신부 측 하객 구분
    @NotNull(message = "신랑 측 또는 신부 측을 선택해 주세요.")
    private RsvpGuestSide guestSide;

    // 수정할 하객 본인의 참석 여부
    @NotNull(message = "참석 여부를 선택해 주세요.")
    private Boolean attending;

    // 본인을 제외한 동반 참석 인원
    @NotNull(message = "동반 인원을 입력해 주세요.")
    @PositiveOrZero(message = "동반 인원은 0명 이상이어야 합니다.")
    private Integer companionCount;

    // 본인과 동반인 중 식사할 인원
    @NotNull(message = "식사 인원을 입력해 주세요.")
    @PositiveOrZero(message = "식사 인원은 0명 이상이어야 합니다.")
    private Integer mealCount;
}
