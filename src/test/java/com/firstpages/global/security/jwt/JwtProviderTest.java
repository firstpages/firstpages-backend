package com.firstpages.global.security.jwt;

import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.response.code.JwtErrorCode;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class JwtProviderTest {

    private JwtProvider jwtProvider;

    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        // 테스트 전용 HS256 서명 키 생성
        secretKey = Jwts.SIG.HS256.key().build();

        // JwtProperties에서 사용하는 Base64 문자열 생성
        String encodedSecret = Base64.getEncoder()
                .encodeToString(secretKey.getEncoded());

        JwtProperties properties = new JwtProperties(
                encodedSecret,
                1800000L,
                1209600000L
        );

        jwtProvider = new JwtProvider(properties);
    }

    @Test
    @DisplayName("Access Token을 발급하고 사용자 ID를 추출한다")
    void createAccessToken() {
        // given
        Long userId = 1L;

        // when
        String token = jwtProvider.createAccessToken(userId);

        Long extractedUserId = jwtProvider.getUserId(
                token,
                JwtTokenType.ACCESS
        );

        // then
        assertThat(token).isNotBlank();
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("Refresh Token을 발급하고 사용자 ID를 추출한다")
    void createRefreshToken() {
        // given
        Long userId = 1L;

        // when
        String token = jwtProvider.createRefreshToken(userId);

        Long extractedUserId = jwtProvider.getUserId(
                token,
                JwtTokenType.REFRESH
        );

        // then
        assertThat(token).isNotBlank();
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("토큰 유형이 일치하지 않으면 예외가 발생한다")
    void invalidTokenType() {
        // given
        String refreshToken = jwtProvider.createRefreshToken(1L);

        // when & then
        assertThatThrownBy(() ->
                jwtProvider.getUserId(
                        refreshToken,
                        JwtTokenType.ACCESS
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException e = (BusinessException) exception;

                    assertThat(e.getErrorCode())
                            .isEqualTo(JwtErrorCode.INVALID_TOKEN_TYPE);
                });
    }

    @Test
    @DisplayName("만료된 JWT는 EXPIRED_TOKEN 예외가 발생한다")
    void expiredToken() {
        // given
        Instant now = Instant.now();

        String token = Jwts.builder()
                .subject("1")
                .claim("type", JwtTokenType.ACCESS.name())
                .issuedAt(Date.from(now.minusSeconds(3600)))
                .expiration(Date.from(now.minusSeconds(1800)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        // when & then
        assertThatThrownBy(() ->
                jwtProvider.getUserId(
                        token,
                        JwtTokenType.ACCESS
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException e = (BusinessException) exception;

                    assertThat(e.getErrorCode())
                            .isEqualTo(JwtErrorCode.EXPIRED_TOKEN);
                });
    }

    @Test
    @DisplayName("서명이 다른 JWT는 INVALID_TOKEN 예외가 발생한다")
    void invalidSignature() {
        // given
        SecretKey otherKey = Jwts.SIG.HS256.key().build();

        String token = Jwts.builder()
                .subject("1")
                .claim("type", JwtTokenType.ACCESS.name())
                .expiration(Date.from(
                        Instant.now().plusSeconds(1800)
                ))
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        // when & then
        assertThatThrownBy(() ->
                jwtProvider.getUserId(
                        token,
                        JwtTokenType.ACCESS
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException e = (BusinessException) exception;

                    assertThat(e.getErrorCode())
                            .isEqualTo(JwtErrorCode.INVALID_TOKEN);
                });
    }

    @Test
    @DisplayName("JWT 형식이 잘못되면 INVALID_TOKEN 예외가 발생한다")
    void malformedToken() {
        // given
        String token = "invalid.jwt.token";

        // when & then
        assertThatThrownBy(() ->
                jwtProvider.getUserId(
                        token,
                        JwtTokenType.ACCESS
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException e = (BusinessException) exception;

                    assertThat(e.getErrorCode())
                            .isEqualTo(JwtErrorCode.INVALID_TOKEN);
                });
    }

    @Test
    @DisplayName("사용자 ID가 없는 JWT는 INVALID_TOKEN 예외가 발생한다")
    void missingUserId() {
        // given
        String token = Jwts.builder()
                .claim("type", JwtTokenType.ACCESS.name())
                .expiration(Date.from(
                        Instant.now().plusSeconds(1800)
                ))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        // when & then
        assertThatThrownBy(() ->
                jwtProvider.getUserId(
                        token,
                        JwtTokenType.ACCESS
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException e = (BusinessException) exception;

                    assertThat(e.getErrorCode())
                            .isEqualTo(JwtErrorCode.INVALID_TOKEN);
                });
    }

    @Test
    @DisplayName("사용자 ID가 숫자가 아니면 INVALID_TOKEN 예외가 발생한다")
    void invalidUserId() {
        // given
        String token = Jwts.builder()
                .subject("invalid-user-id")
                .claim("type", JwtTokenType.ACCESS.name())
                .expiration(Date.from(
                        Instant.now().plusSeconds(1800)
                ))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        // when & then
        assertThatThrownBy(() ->
                jwtProvider.getUserId(
                        token,
                        JwtTokenType.ACCESS
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException e = (BusinessException) exception;

                    assertThat(e.getErrorCode())
                            .isEqualTo(JwtErrorCode.INVALID_TOKEN);
                });
    }
}