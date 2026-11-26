package com.firstpages.domain.guestbook.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 하객이 제출하는 방명록 작성 요청 DTO
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class GuestbookCreateRequestDto {

    // 하객이 입력한 작성자 이름
    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
    private String authorName;

    // 하객이 남기는 축하 메시지 본문
    @NotBlank(message = "축하 메시지를 입력해 주세요.")
    @Size(max = 500, message = "축하 메시지는 500자 이하여야 합니다.")
    private String message;
}
