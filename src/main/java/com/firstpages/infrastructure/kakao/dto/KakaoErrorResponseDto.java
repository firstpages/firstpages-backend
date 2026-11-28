package com.firstpages.infrastructure.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 카카오 연결 해제 오류 응답
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoErrorResponseDto {

    // HTTP 상태와 별도로 제공되는 카카오 오류 코드
    private Integer code;
}
