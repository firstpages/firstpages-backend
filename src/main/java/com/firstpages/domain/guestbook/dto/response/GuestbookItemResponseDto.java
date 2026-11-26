package com.firstpages.domain.guestbook.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.firstpages.domain.guestbook.entity.Guestbook;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 소유자의 방명록 관리 목록에 표시할 방명록 메시지 한 건 응답 DTO
@JsonPropertyOrder({
        "id",
        "authorName",
        "message",
        "createdAt"
})
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GuestbookItemResponseDto {

    // 방명록 메시지 식별자
    private final Long id;

    // 하객이 입력한 작성자 이름
    private final String authorName;

    // 하객이 남긴 축하 메시지 본문
    private final String message;

    // 방명록 메시지 작성 일시
    private final LocalDateTime createdAt;

    // 방명록 엔티티를 관리 목록의 응답 항목으로 변환
    public static GuestbookItemResponseDto from(Guestbook guestbook) {
        return GuestbookItemResponseDto.builder()
                .id(guestbook.getId())
                .authorName(guestbook.getAuthorName())
                .message(guestbook.getMessage())
                .createdAt(guestbook.getCreatedAt())
                .build();
    }

}
