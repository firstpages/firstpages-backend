package com.firstpages.global.security.jwt;

import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.response.code.JwtErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "type";

    private final SecretKey secretKey;

    private final JwtProperties properties;

    public JwtProvider(JwtProperties properties) {
        this.properties = properties;

        // Base64 인코딩된 비밀키를 디코딩
        byte[] bytes = Decoders.BASE64.decode(properties.getSecret());

        // HS256은 최소 256비트 키가 필요
        if (bytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret key must be at least 256 bits."
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(bytes);
    }

    // JWT 공통 생성 로직
    private String createToken(
            Long userId,
            JwtTokenType tokenType,
            long expirationMillis
    ) {
        // 현재 시점
        Instant now = Instant.now();

        // 현재 시점 기준 만료 시각
        Instant expiration = now.plusMillis(expirationMillis);

        return Jwts.builder()
                // 토큰마다 고유한 식별자 생성
                .id(UUID.randomUUID().toString())
                // 토큰의 주인
                .subject(userId.toString())
                // 토큰 타입 클레임
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                // 발행 시간
                .issuedAt(Date.from(now))
                // 만료 시간
                .expiration(Date.from(expiration))
                // 암호화 서명
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    // Access Token 발급
    public String createAccessToken(Long userId) {
        return createToken(
                userId,
                JwtTokenType.ACCESS,
                properties.getAccessExpiration()
        );
    }

    // Refresh Token 발급
    public String createRefreshToken(Long userId) {
        return createToken(
                userId,
                JwtTokenType.REFRESH,
                properties.getRefreshExpiration()
        );
    }

    // 토큰 검증 및 클레임 반환
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    // 시크릿 키로 서명 확인
                    .verifyWith(secretKey)
                    .build()
                    // 토큰 파싱
                    .parseSignedClaims(token)
                    // 데이터 꺼내기
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(
                    JwtErrorCode.EXPIRED_TOKEN
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(
                    JwtErrorCode.INVALID_TOKEN
            );
        }
    }

    // 토큰에서 사용자 ID 추출
    public Long getUserId(
            String token,
            JwtTokenType expectedTokenType
    ) {
        // 클레임 추출
        Claims claims = parseClaims(token);

        // 클레임에서 토큰 타입 추출
        String tokenType = claims.get(
                TOKEN_TYPE_CLAIM,
                String.class
        );

        // 토큰의 타입이 동일한지 비교
        if (!expectedTokenType.name().equals(tokenType)) {
            throw new BusinessException(
                    JwtErrorCode.INVALID_TOKEN_TYPE
            );
        }

        // 검증하고 추출
        return validateUserId(claims);
    }

    // 올바른 사용자 ID인지 검증
    private Long validateUserId(Claims claims) {
        try {
            // 없을 경우를 대비한 null 예외 처리 (NullPointerException)
            // 클레임에서 Sub 추출
            String subject = claims.getSubject();

            // 숫자가 아닐 경우 예외 처리(NumberFormatException)
            // 사용자 ID
            Long userId = Long.valueOf(subject);

            // 추가로 음수일 경우 예외 처리(NumberFormatException)
            if (userId <= 0) {
                throw new NumberFormatException("Invalid user id");
            }

            return userId;
        } catch (NumberFormatException | NullPointerException e) {
            throw new BusinessException(
                    JwtErrorCode.INVALID_TOKEN
            );
        }
    }
}
