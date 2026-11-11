package com.firstpages.domain.auth.service;

import com.firstpages.domain.auth.dto.response.AuthTokenPairDto;
import com.firstpages.domain.auth.exception.AuthErrorCode;
import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.security.jwt.JwtProvider;
import com.firstpages.global.security.jwt.JwtTokenType;
import com.firstpages.global.security.oauth2.OAuth2CookieManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Refresh Token 검증과 JWT 재발급 흐름을 담당
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtProvider jwtProvider;

    private final RefreshTokenService refreshTokenService;

    // Refresh Token으로 새 Access Token과 Refresh Token을 발급

    @Override
    public AuthTokenPairDto reissue(String refreshToken) {

        // Refresh Token이 없으면 재발급 요청 거부
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
            );
        }

        // JWT의 서명, 만료시간, REFRESH 유형을 검증하고 사용자 ID 추출
        Long userId = jwtProvider.getUserId(
                refreshToken,
                JwtTokenType.REFRESH
        );

        // 사용자 ID를 기반으로 새로운 JWT 발급
        String newAccessToken = jwtProvider.createAccessToken(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        // 이전 Refresh Token이 유효한 경우에만 Redis 값을 교체
        boolean rotated = refreshTokenService.rotate(
                userId,
                refreshToken,
                newRefreshToken
        );

        // Redis에 등록되지 않았거나 이미 사용된 토큰은 거부
        if (!rotated) {
            throw new BusinessException(
                    AuthErrorCode.INVALID_REFRESH_TOKEN
            );
        }

        // Redis 교체에 성공한 경우에만 새 JWT를 반환
        return new AuthTokenPairDto(
                newAccessToken,
                newRefreshToken
        );
    }

    // 현재 브라우저의 인증 상태를 종료
    @Override
    public void logout(String refreshToken) {

        // Refresh Token이 없다면 서버에서 폐기할 대상이 없음
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        Long userId;

        try {
            // JWT 서명, 만료시간 및 REFRESH 유형 검증
            userId = jwtProvider.getUserId(
                    refreshToken,
                    JwtTokenType.REFRESH
            );
        } catch (BusinessException e) {
            // 이미 만료되었거나 유효하지 않은 토큰은 Redis의 다른 토큰을 삭제하지 않고 종료
            return;
        }

        // 해당 Refresh Token이 현재 저장된 토큰인 경우에만 폐기
        refreshTokenService.deleteIfMatches(
                userId,
                refreshToken
        );
    }
}
