package com.firstpages.domain.auth.repository;

import java.time.Duration;
import java.util.Optional;

// Refresh Token 저장소의 데이터 접근 기능을 정의
public interface RefreshTokenRepository {

    // 사용자 ID를 기준으로 Refresh Token 해시값과 TTL 저장
    void save(Long userId, String tokenHash, Duration ttl);

    // 사용자 ID에 연결된 Refresh Token 해시값 조회
    Optional<String> findByUserId(Long userId);

    // 사용자 ID에 연결된 Refresh Token 데이터 삭제
    void deleteByUserId(Long userId);

    // 기존 해시값이 일치할 때만 새로운 해시값으로 원자적으로 교체
    boolean rotate(Long userId, String expectedHash, String newHash, Duration ttl);

    // 저장된 해시값이 일치할 때만 Refresh Token 삭제
    boolean deleteIfMatches(Long userId, String expectedHash);
}
