package com.firstpages.domain.auth.service;

import com.firstpages.domain.auth.repository.RefreshTokenRepository;
import com.firstpages.global.security.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

// Refresh Token 해싱과 저장 및 교체 정책을 구현
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    private final JwtProperties jwtProperties;

    // Refresh Token을 해싱하여 Redis에 저장
    @Override
    public void save(
            Long userId,
            String refreshToken
    ) {
        String tokenHash = hash(refreshToken);

        refreshTokenRepository.save(
                userId,
                tokenHash,
                Duration.ofMillis(jwtProperties.getRefreshExpiration())
        );
    }

    // 전달받은 Refresh Token과 Redis에 저장된 토큰의 일치 여부 확인
    @Override
    public boolean isMatching(Long userId, String refreshToken) {
        String storedHash = refreshTokenRepository.findByUserId(userId)
                .orElse(null);

        if (storedHash == null) {
            return false;
        }

        String requestedHash = hash(refreshToken);

        // 해시값을 비교하여 동일한 Refresh Token인지 확인
        return MessageDigest.isEqual(
                requestedHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    // 사용자 ID에 연결된 Refresh Token 저장 정보 제거
    @Override
    public void delete(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Override
    public boolean rotate(
            Long userId,
            String oldRefreshToken,
            String newRefreshToken
    ) {
        String oldHash = hash(oldRefreshToken);
        String newHash = hash(newRefreshToken);

        // JWT의 만료기간과 Redis TTL을 동일하게 유지
        Duration ttl = Duration.ofMillis(jwtProperties.getRefreshExpiration());

        // Redis에서 검증과 교체를 원자적으로 수행
        return refreshTokenRepository.rotate(
                userId,
                oldHash,
                newHash,
                ttl
        );
    }

    // 현재 Redis에 등록된 Refresh Token과 일치할 때만 삭제
    @Override
    public boolean deleteIfMatches(Long userId, String refreshToken) {
        // Refresh Token 원문을 SHA-256 해시값으로 변환
        String tokenHash = hash(refreshToken);

        // Redis에서 비교와 삭제를 원자적으로 수행
        return refreshTokenRepository.deleteIfMatches(
                userId,
                tokenHash
        );
    }

    // Refresh Token 원문을 SHA-256 해시 문자열로 변환
    private String hash(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashed = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", e);
        }
    }

}
