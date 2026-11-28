package com.firstpages.domain.auth.service;

import com.firstpages.domain.auth.dto.response.AuthTokenPairDto;

// 로그인 완료에 필요한 서비스 토큰 발급 계약
public interface LoginTokenIssuer {

    // 내부 회원 ID로 Access Token과 Refresh Token 발급
    AuthTokenPairDto issue(Long userId);
}
