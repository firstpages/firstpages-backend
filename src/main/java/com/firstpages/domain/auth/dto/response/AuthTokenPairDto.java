package com.firstpages.domain.auth.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 새로 발급한 Access Token과 Refresh Token을 묶어 전달
@Getter
@RequiredArgsConstructor
public class AuthTokenPairDto {

    private final String accessToken;

    private final String refreshToken;

}
