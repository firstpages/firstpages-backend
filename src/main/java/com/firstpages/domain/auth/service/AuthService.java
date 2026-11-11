package com.firstpages.domain.auth.service;

import com.firstpages.domain.auth.dto.response.AuthTokenPairDto;

// 인증 도메인의 주요 비즈니스 기능을 정의
public interface AuthService {

    // Refresh Token을 검증하고 새로운 토큰 쌍을 발급
    AuthTokenPairDto reissue(String refreshToken);

    // 현재 브라우저의 Refresh Token을 폐기
    void logout(String refreshToken);
}
