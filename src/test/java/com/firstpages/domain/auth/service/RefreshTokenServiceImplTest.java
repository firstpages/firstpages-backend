
package com.firstpages.domain.auth.service;

import com.firstpages.domain.auth.repository.RefreshTokenRepository;
import com.firstpages.global.security.jwt.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// RefreshTokenServiceImpl의 해싱 및 저장 정책 검증
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    private static final Long USER_ID = 9L;

    // 테스트용 Refresh Token
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";

    // SHA-256으로 계산한 실제 해시값
    private static final String TOKEN_HASH =
            "0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120";

    private static final String OLD_TOKEN_HASH =
            "66e4f4e9739a9ef9a9d6e414cfd05780c4ab0eb03e21fbf90ebf87e76d4db8f6";

    private static final String NEW_TOKEN_HASH =
            "c40dd1765d767caae2588f0ee1de9181d8a44cc9306261eb2c9e526351188338";

    // Refresh Token 유효기간: 14일
    private static final long REFRESH_EXPIRATION =
            Duration.ofDays(14).toMillis();

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    // 각 테스트 실행 전 Refresh Token 만료시간 설정
    @BeforeEach
    void setUp() {
        // 저장 및 Rotation 테스트에서만 만료시간이 필요
        lenient().when(jwtProperties.getRefreshExpiration())
                .thenReturn(REFRESH_EXPIRATION);
    }

    @Test
    @DisplayName("Refresh Token을 SHA-256으로 해싱하여 Redis에 저장한다")
    void saveRefreshToken() {
        // when
        refreshTokenService.save(USER_ID, REFRESH_TOKEN);

        // then
        verify(refreshTokenRepository).save(
                USER_ID,
                TOKEN_HASH,
                Duration.ofMillis(REFRESH_EXPIRATION)
        );

        // Refresh Token 원문을 저장하면 안 됨
        verify(refreshTokenRepository, never()).save(
                eq(USER_ID),
                eq(REFRESH_TOKEN),
                any()
        );
    }

    @Test
    @DisplayName("Redis TTL은 JwtProperties의 만료시간과 일치한다")
    void saveWithCorrectTtl() {
        // when
        refreshTokenService.save(USER_ID, REFRESH_TOKEN);

        // then
        verify(jwtProperties).getRefreshExpiration();

        verify(refreshTokenRepository).save(
                USER_ID,
                TOKEN_HASH,
                Duration.ofDays(14)
        );
    }

    @Test
    @DisplayName("Redis에 저장된 해시값과 일치하면 true를 반환한다")
    void isMatchingSuccess() {
        // given
        when(refreshTokenRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(TOKEN_HASH));

        // when
        boolean result = refreshTokenService.isMatching(
                USER_ID,
                REFRESH_TOKEN
        );

        // then
        assertTrue(result);

        verify(refreshTokenRepository).findByUserId(USER_ID);
    }

    @Test
    @DisplayName("Redis에 저장된 해시값과 다르면 false를 반환한다")
    void isMatchingFailure() {
        // given
        when(refreshTokenRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(OLD_TOKEN_HASH));

        // when
        boolean result = refreshTokenService.isMatching(
                USER_ID,
                REFRESH_TOKEN
        );

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("Redis에 Refresh Token이 없으면 false를 반환한다")
    void isMatchingWithoutStoredToken() {
        // given
        when(refreshTokenRepository.findByUserId(USER_ID))
                .thenReturn(Optional.empty());

        // when
        boolean result = refreshTokenService.isMatching(
                USER_ID,
                REFRESH_TOKEN
        );

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("기존 토큰과 새 토큰을 해싱하여 Rotation을 요청한다")
    void rotateSuccess() {
        // given
        when(refreshTokenRepository.rotate(
                USER_ID,
                OLD_TOKEN_HASH,
                NEW_TOKEN_HASH,
                Duration.ofDays(14)
        )).thenReturn(true);

        // when
        boolean result = refreshTokenService.rotate(
                USER_ID,
                OLD_REFRESH_TOKEN,
                NEW_REFRESH_TOKEN
        );

        // then
        assertTrue(result);

        verify(refreshTokenRepository).rotate(
                USER_ID,
                OLD_TOKEN_HASH,
                NEW_TOKEN_HASH,
                Duration.ofDays(14)
        );
    }

    @Test
    @DisplayName("Redis Rotation이 실패하면 false를 반환한다")
    void rotateFailure() {
        // given
        when(refreshTokenRepository.rotate(
                USER_ID,
                OLD_TOKEN_HASH,
                NEW_TOKEN_HASH,
                Duration.ofDays(14)
        )).thenReturn(false);

        // when
        boolean result = refreshTokenService.rotate(
                USER_ID,
                OLD_REFRESH_TOKEN,
                NEW_REFRESH_TOKEN
        );

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("현재 Refresh Token이 일치하면 조건부 삭제에 성공한다")
    void deleteIfMatchesSuccess() {
        // given
        when(refreshTokenRepository.deleteIfMatches(
                USER_ID,
                TOKEN_HASH
        )).thenReturn(true);

        // when
        boolean result = refreshTokenService.deleteIfMatches(
                USER_ID,
                REFRESH_TOKEN
        );

        // then
        assertTrue(result);

        verify(refreshTokenRepository).deleteIfMatches(
                USER_ID,
                TOKEN_HASH
        );
    }

    @Test
    @DisplayName("현재 Refresh Token이 일치하지 않으면 삭제에 실패한다")
    void deleteIfMatchesFailure() {
        // given
        when(refreshTokenRepository.deleteIfMatches(
                USER_ID,
                TOKEN_HASH
        )).thenReturn(false);

        // when
        boolean result = refreshTokenService.deleteIfMatches(
                USER_ID,
                REFRESH_TOKEN
        );

        // then
        assertFalse(result);

        // 다른 토큰을 무조건 삭제하면 안 됨
        verify(refreshTokenRepository, never())
                .deleteByUserId(USER_ID);
    }

    @Test
    @DisplayName("사용자 ID로 저장된 Refresh Token을 삭제한다")
    void deleteRefreshToken() {
        // when
        refreshTokenService.delete(USER_ID);

        // then
        verify(refreshTokenRepository).deleteByUserId(USER_ID);

        // 삭제 요청은 정확히 한 번만 수행
        verify(refreshTokenRepository, times(1))
                .deleteByUserId(USER_ID);
    }
}
