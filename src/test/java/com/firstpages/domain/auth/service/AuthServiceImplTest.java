package com.firstpages.domain.auth.service;

import com.firstpages.domain.auth.dto.response.AuthTokenPairDto;
import com.firstpages.domain.auth.exception.AuthErrorCode;
import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.response.code.JwtErrorCode;
import com.firstpages.global.security.jwt.JwtProvider;
import com.firstpages.global.security.jwt.JwtTokenType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// AuthServiceImpl의 인증 비즈니스 로직을 검증
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("정상적인 Refresh Token으로 JWT를 재발급한다")
    void reissueSuccess() {
        // given
        Long userId = 9L;

        String oldRefreshToken = "old-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        // JWT 검증 결과로 사용자 ID가 반환되도록 설정
        when(jwtProvider.getUserId(
                oldRefreshToken,
                JwtTokenType.REFRESH
        )).thenReturn(userId);

        // 새 JWT 생성 결과 설정
        when(jwtProvider.createAccessToken(userId))
                .thenReturn(newAccessToken);

        when(jwtProvider.createRefreshToken(userId))
                .thenReturn(newRefreshToken);

        // Redis Rotation이 성공하도록 설정
        when(refreshTokenService.rotate(
                userId,
                oldRefreshToken,
                newRefreshToken
        )).thenReturn(true);

        // when
        AuthTokenPairDto result = authService.reissue(oldRefreshToken);

        // then
        assertEquals(newAccessToken, result.getAccessToken());
        assertEquals(newRefreshToken, result.getRefreshToken());

        // 이전 Refresh Token이 새 토큰으로 교체되었는지 확인
        verify(refreshTokenService).rotate(
                userId,
                oldRefreshToken,
                newRefreshToken
        );
    }

    @Test
    @DisplayName("Refresh Token이 없으면 재발급을 거부한다")
    void reissueWithoutRefreshToken() {
        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.reissue(null)
        );

        // then
        assertEquals(
                AuthErrorCode.REFRESH_TOKEN_NOT_FOUND,
                exception.getErrorCode()
        );

        // 토큰이 없으므로 JWT 검증 및 Redis 접근이 없어야 함
        verifyNoInteractions(jwtProvider, refreshTokenService);
    }

    @Test
    @DisplayName("Refresh Token이 빈 문자열이면 재발급을 거부한다")
    void reissueWithBlankRefreshToken() {
        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.reissue("   ")
        );

        // then
        assertEquals(
                AuthErrorCode.REFRESH_TOKEN_NOT_FOUND,
                exception.getErrorCode()
        );

        verifyNoInteractions(jwtProvider, refreshTokenService);
    }

    @Test
    @DisplayName("만료된 Refresh Token이면 재발급을 거부한다")
    void reissueWithExpiredRefreshToken() {
        // given
        String expiredToken = "expired-refresh-token";

        // JWT 검증 과정에서 만료 예외 발생
        when(jwtProvider.getUserId(
                expiredToken,
                JwtTokenType.REFRESH
        )).thenThrow(
                new BusinessException(JwtErrorCode.EXPIRED_TOKEN)
        );

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.reissue(expiredToken)
        );

        // then
        assertEquals(
                JwtErrorCode.EXPIRED_TOKEN,
                exception.getErrorCode()
        );

        // 만료된 토큰은 Redis Rotation을 실행하지 않음
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("이미 사용된 Refresh Token은 재발급을 거부한다")
    void reissueWithReusedRefreshToken() {
        // given
        Long userId = 9L;

        String oldRefreshToken = "old-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        when(jwtProvider.getUserId(
                oldRefreshToken,
                JwtTokenType.REFRESH
        )).thenReturn(userId);

        when(jwtProvider.createAccessToken(userId))
                .thenReturn(newAccessToken);

        when(jwtProvider.createRefreshToken(userId))
                .thenReturn(newRefreshToken);

        // Redis에 저장된 토큰과 불일치하는 상황
        when(refreshTokenService.rotate(
                userId,
                oldRefreshToken,
                newRefreshToken
        )).thenReturn(false);

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.reissue(oldRefreshToken)
        );

        // then
        assertEquals(
                AuthErrorCode.INVALID_REFRESH_TOKEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("정상적인 Refresh Token으로 로그아웃하면 Redis 토큰을 폐기한다")
    void logoutSuccess() {
        // given
        Long userId = 9L;
        String refreshToken = "valid-refresh-token";

        when(jwtProvider.getUserId(
                refreshToken,
                JwtTokenType.REFRESH
        )).thenReturn(userId);

        // when
        authService.logout(refreshToken);

        // then
        verify(refreshTokenService).deleteIfMatches(
                userId,
                refreshToken
        );
    }

    @Test
    @DisplayName("Refresh Token이 없어도 로그아웃을 정상 처리한다")
    void logoutWithoutRefreshToken() {
        // when
        assertDoesNotThrow(() -> authService.logout(null));

        // then
        verifyNoInteractions(jwtProvider, refreshTokenService);
    }

    @Test
    @DisplayName("만료된 Refresh Token으로 로그아웃하면 Redis 토큰을 삭제하지 않는다")
    void logoutWithExpiredRefreshToken() {
        // given
        String expiredToken = "expired-refresh-token";

        when(jwtProvider.getUserId(
                expiredToken,
                JwtTokenType.REFRESH
        )).thenThrow(
                new BusinessException(JwtErrorCode.EXPIRED_TOKEN)
        );

        // when
        assertDoesNotThrow(() -> authService.logout(expiredToken));

        // then
        verifyNoInteractions(refreshTokenService);
    }
}