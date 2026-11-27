package com.firstpages.domain.rsvp.dto.request;

import com.firstpages.domain.rsvp.entity.RsvpGuestSide;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RsvpCreateRequestDto {

    // 하객이 입력한 이름
    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 20, message = "이름은 최대 20자입니다.")
    private String guestName;

    // 신랑 측 또는 신부 측 하객 구분
    @NotNull(message = "신랑 측 또는 신부 측을 선택해 주세요.")
    private RsvpGuestSide guestSide;

    // 앞자리 0을 유지하는 휴대전화 번호 뒷 네 자리
    @NotBlank(message = "휴대전화 번호 뒷 네 자리를 입력해 주세요.")
    @Pattern(regexp = "[0-9]{4}", message = "휴대전화 번호 뒷 네 자리를 숫자로 입력해 주세요.")
    private String phoneLastFour;

    // 하객 본인의 참석 여부
    @NotNull(message = "참석 여부를 선택해 주세요.")
    private Boolean attending;

    // 하객 본인을 제외한 동반 인원
    @PositiveOrZero(message = "동반 인원은 0명 이상이어야 합니다.")
    private Integer companionCount;

    // 본인과 동반인 중 식사할 인원
    @PositiveOrZero(message = "식사 인원은 0명 이상이어야 합니다.")
    private Integer mealCount;

}
