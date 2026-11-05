package com.firstpages.global.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({
        "page",
        "size",
        "totalElements",
        "totalPages",
        "items"
})
public class PageResponse<T> {

    private final int page;

    private final int size;

    private final long totalElements;

    private final int totalPages;

    private final List<T> items;

    // Spring Data의 Page 객체를 공통 페이지 응답으로 변환합니다.
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getContent()
        );
    }
}
