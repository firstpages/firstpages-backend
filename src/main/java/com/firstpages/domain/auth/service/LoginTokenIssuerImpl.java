package com.firstpages.domain.auth.service;

import com.firstpages.domain.auth.dto.response.AuthTokenPairDto;
import com.firstpages.domain.user.repository.UserRepository;
import com.firstpages.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 로그인 토큰을 발급하는 서비스
@Service
@RequiredArgsConstructor
public class LoginTokenIssuerImpl implements LoginTokenIssuer {

    // 토큰 발급 직전 회원 존재 확인에 사용하는 저장소
    private final UserRepository userRepository;

    // Access Token과 Refresh Token 생성기
    private final JwtProvider jwtProvider;

    // 새 Refresh Token을 TTL과 함께 저장하는 서비스
    private final RefreshTokenService refreshTokenService;

    // 회원을 잠그고 토큰을 생성한 뒤 Refresh Token 저장
    @Transactional
    @Override
    public AuthTokenPairDto issue(Long userId) {
        // OAuth 사용자 조회 후 회원이 탈퇴했으면 발급 차단
        userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("account_unavailable")
                ));

        // 내부 회원 ID로 서비스 토큰 생성
        String accessToken = jwtProvider.createAccessToken(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        // 회원 락을 유지한 채 Redis에 Refresh Token 저장
        refreshTokenService.save(userId, refreshToken);

        // 저장을 마친 토큰을 성공 핸들러에 반환
        return new AuthTokenPairDto(
                accessToken,
                refreshToken
        );
    }
}
