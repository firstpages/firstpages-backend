package com.firstpages.global.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    // 시크릿 키
    @NotBlank
    private final String secret;

    // 액세스 토큰 만료 시간
    @Positive
    private final long accessExpiration;

    // 리프레쉬 토큰 만료 시간
    @Positive
    private final long refreshExpiration;
}
