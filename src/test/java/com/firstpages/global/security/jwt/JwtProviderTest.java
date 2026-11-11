
package com.firstpages.global.security.jwt;

import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.response.code.JwtErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// JWT 발급 및 검증 로직의 단위 테스트
@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    private static final Long USER_ID = 9L;

    // HS256에서 요구하는 256비트 테스트 전용 비밀키
    private static final byte[] SECRET_BYTES =
            "0123456789abcdef0123456789abcdef"
                    .getBytes(StandardCharsets.UTF_8);

    private static final String SECRET =
            Encoders.BASE64.encode(SECRET_BYTES);

    private static final long ACCESS_EXPIRATION =
            Duration.ofMinutes(30).toMillis();

    private static final long REFRESH_EXPIRATION =
            Duration.ofDays(14).toMillis();

    @Mock
    private JwtProperties properties;

    private JwtProvider jwtProvider;

    // 각 테스트마다 JwtProvider를 새로 생성
    @BeforeEach
    void setUp() {
        lenient().when(properties.getSecret())
                .thenReturn(SECRET);

        lenient().when(properties.getAccessExpiration())
                .thenReturn(ACCESS_EXPIRATION);

        lenient().when(properties.getRefreshExpiration())
                .thenReturn(REFRESH_EXPIRATION);

        jwtProvider = new JwtProvider(properties);
    }

    @Test
    @DisplayName("Access Token을 정상적으로 발급한다")
    void createAccessToken() {
        // when
        String token = jwtProvider.createAccessToken(USER_ID);

        // then
        Claims claims = parseClaims(token);

        assertEquals(USER_ID.toString(), claims.getSubject());
        assertEquals("ACCESS", claims.get("type", String.class));

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        // 발급된 토큰에 jti가 존재해야 함
        assertNotNull(claims.getId());
        assertFalse(claims.getId().isBlank());
    }

    @Test
    @DisplayName("Refresh Token을 정상적으로 발급한다")
    void createRefreshToken() {
        // when
        String token = jwtProvider.createRefreshToken(USER_ID);

        // then
        Claims claims = parseClaims(token);

        assertEquals(USER_ID.toString(), claims.getSubject());
        assertEquals("REFRESH", claims.get("type", String.class));

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getId());
    }

    @Test
    @DisplayName("동일한 사용자에게 발급한 Refresh Token은 서로 다른 jti를 가진다")
    void refreshTokenHasUniqueJti() {
        // when
        String firstToken = jwtProvider.createRefreshToken(USER_ID);
        String secondToken = jwtProvider.createRefreshToken(USER_ID);

        // then
        Claims firstClaims = parseClaims(firstToken);
        Claims secondClaims = parseClaims(secondToken);

        // JWT마다 고유한 식별자가 생성되는지 검증
        assertNotEquals(
                firstClaims.getId(),
                secondClaims.getId()
        );

        // jti가 다르므로 JWT 문자열도 달라야 함
        assertNotEquals(firstToken, secondToken);
    }

    @Test
    @DisplayName("JWT는 HS256 알고리즘으로 서명된다")
    void tokenUsesHs256() {
        // given
        String token = jwtProvider.createAccessToken(USER_ID);

        // when
        String algorithm = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getHeader()
                .getAlgorithm();

        // then
        assertEquals("HS256", algorithm);
    }

    @Test
    @DisplayName("Access Token에서 사용자 ID를 정상적으로 추출한다")
    void getUserIdFromAccessToken() {
        // given
        String token = jwtProvider.createAccessToken(USER_ID);

        // when
        Long result = jwtProvider.getUserId(
                token,
                JwtTokenType.ACCESS
        );

        // then
        assertEquals(USER_ID, result);
    }

    @Test
    @DisplayName("Refresh Token에서 사용자 ID를 정상적으로 추출한다")
    void getUserIdFromRefreshToken() {
        // given
        String token = jwtProvider.createRefreshToken(USER_ID);

        // when
        Long result = jwtProvider.getUserId(
                token,
                JwtTokenType.REFRESH
        );

        // then
        assertEquals(USER_ID, result);
    }

    @Test
    @DisplayName("Access Token을 Refresh Token으로 사용하면 검증에 실패한다")
    void invalidTokenType() {
        // given
        String accessToken = jwtProvider.createAccessToken(USER_ID);

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> jwtProvider.getUserId(
                        accessToken,
                        JwtTokenType.REFRESH
                )
        );

        // then
        assertEquals(
                JwtErrorCode.INVALID_TOKEN_TYPE,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("만료된 JWT를 사용하면 검증에 실패한다")
    void expiredToken() {
        // given
        // 이미 만료된 토큰을 생성하도록 만료기간 변경
        when(properties.getAccessExpiration())
                .thenReturn(-Duration.ofMinutes(1).toMillis());

        JwtProvider expiredTokenProvider = new JwtProvider(properties);

        String token = expiredTokenProvider.createAccessToken(USER_ID);

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> jwtProvider.getUserId(
                        token,
                        JwtTokenType.ACCESS
                )
        );

        // then
        assertEquals(
                JwtErrorCode.EXPIRED_TOKEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("JWT 형식이 올바르지 않으면 검증에 실패한다")
    void malformedToken() {
        // given
        String token = "invalid-jwt-token";

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> jwtProvider.getUserId(
                        token,
                        JwtTokenType.ACCESS
                )
        );

        // then
        assertEquals(
                JwtErrorCode.INVALID_TOKEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("다른 비밀키로 서명한 JWT는 검증에 실패한다")
    void invalidSignature() {
        // given
        byte[] otherSecretBytes =
                "abcdef0123456789abcdef0123456789"
                        .getBytes(StandardCharsets.UTF_8);

        SecretKey otherSecret = Keys.hmacShaKeyFor(otherSecretBytes);

        Instant now = Instant.now();

        String token = Jwts.builder()
                .subject(USER_ID.toString())
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(1800)))
                .signWith(otherSecret, Jwts.SIG.HS256)
                .compact();

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> jwtProvider.getUserId(
                        token,
                        JwtTokenType.ACCESS
                )
        );

        // then
        assertEquals(
                JwtErrorCode.INVALID_TOKEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("사용자 ID가 숫자가 아니면 검증에 실패한다")
    void invalidUserId() {
        // given
        String token = createTokenWithSubject("invalid-user");

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> jwtProvider.getUserId(
                        token,
                        JwtTokenType.ACCESS
                )
        );

        // then
        assertEquals(
                JwtErrorCode.INVALID_TOKEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("사용자 ID가 0 이하이면 검증에 실패한다")
    void nonPositiveUserId() {
        // given
        String token = createTokenWithSubject("-1");

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> jwtProvider.getUserId(
                        token,
                        JwtTokenType.ACCESS
                )
        );

        // then
        assertEquals(
                JwtErrorCode.INVALID_TOKEN,
                exception.getErrorCode()
        );
    }

    // 실제 JWT의 서명을 검증하고 Claims 추출
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 테스트용 비밀키 생성
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(SECRET_BYTES);
    }

    // 사용자 ID 클레임에 임의 값을 넣은 JWT 생성
    private String createTokenWithSubject(String subject) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(subject)
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(1800)))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }
}
