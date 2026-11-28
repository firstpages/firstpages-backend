package com.firstpages.infrastructure.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 카카오 연결 해제 성공 응답
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnlinkResponseDto {

    // 연결이 해제된 카카오 회원번호
    private Long id;
}
