package com.firstpages.domain.auth.service;

// Refresh Token의 저장, 검증, 삭제 기능을 정의하는 인터페이스
public interface RefreshTokenService {

    // 발급한 Refresh Token을 서버 저장소에 등록
    void save(Long userId, String refreshToken);

    // 요청한 Refresh Token이 서버에 등록된 토큰과 일치하는지 확인
    boolean isMatching(Long userId, String refreshToken);

    // 기존 Refresh Token과 일치할 때만 새로운 토큰으로 교체
    boolean rotate(Long userId, String oldRefreshToken, String newRefreshToken);

    // 사용자에 연결된 Refresh Token 정보 삭제
    void delete(Long userId);

    // Refresh Token이 현재 저장된 값과 일치할 때만 폐기
    boolean deleteIfMatches(Long userId, String refreshToken);
}
