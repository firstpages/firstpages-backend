package com.firstpages.global.config;

import com.firstpages.global.security.SecurityErrorResponseWriter;
import com.firstpages.global.security.jwt.JwtAccessDeniedHandler;
import com.firstpages.global.security.jwt.JwtAuthenticationEntryPoint;
import com.firstpages.global.security.jwt.JwtAuthenticationFilter;
import com.firstpages.global.security.jwt.JwtProvider;
import com.firstpages.global.security.oauth2.CustomOAuth2UserService;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationFailureHandler;
import com.firstpages.global.security.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // OAuth2 로그인 전용 필터 체인
    @Bean
    @Order(1)
    public SecurityFilterChain oauthSecurityFilterChain(
            HttpSecurity http,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2AuthenticationSuccessHandler successHandler,
            OAuth2AuthenticationFailureHandler failureHandler
    ) throws Exception {
        http
                // OAuth2 체인이 처리할 URL 범위를 제한
                .securityMatcher(
                        "/oauth2/authorization/**",
                        "/login/oauth2/code/**"
                )
                .csrf(AbstractHttpConfigurer::disable)
                // 필요한 경우 HTTP 세션을 생성
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> auth
                        .anyRequest()
                        .permitAll()
                )
                // 기본 OAuth2 로그인 기능을 활성화
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        // 로그인 성공 시 JWT 발급 및 쿠키 설정
                        .successHandler(successHandler)
                        // 로그인 실패 시 임시 세션 정리 및 리다이렉트
                        .failureHandler(failureHandler)
                );

        return http.build();
    }

    // JWT 기반 일반 API 필터 체인
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            JwtProvider jwtProvider,
            SecurityErrorResponseWriter responseWriter,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtProvider,
                responseWriter
        );

        http
                // 교차 출처 요청을 처리하도록 활성화
                .cors(Customizer.withDefaults())
                // 쿠키 자동 전송에 따른 CSRF 공격 방어
                .csrf(CsrfConfigurer::spa)
                // Form Login 및 HTTP Basic 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 세션을 이용해 SecurityContext를 유지하지 않도록 설정
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 요청에 대한 인가 규칙을 설정
                .authorizeHttpRequests(auth -> auth
                        // 인가 규칙을 적용할 요청
                        .requestMatchers(
                                "/actuator/health",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/csrf"
                        )
                        .permitAll()
                        // Refresh Token을 이용한 인증 관련 API
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/reissue",
                                "/api/auth/logout"
                        )
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        )
                        .permitAll()
                        // 앞선 규칙과 일치하지 않은 나머지 요청
                        .anyRequest()
                        // 인증된 사용자만 접근 허용
                        .authenticated()
                )
                .exceptionHandling(exception -> exception
                        // 401 처리
                        .authenticationEntryPoint(authenticationEntryPoint)
                        // 403 처리
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // Spring Security 필터 체인에 JWT 인증 필터를 등록
                // UsernamePasswordAuthenticationFilter보다 앞에서 실행되도록 순서를 지정
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // 클라이언트의 교차 출처 API 호출에 적용할 CORS 정책
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}")
            List<String> allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();

        // 접근을 허용할 Origin 지정
        config.setAllowedOrigins(allowedOrigins);

        // CORS 요청에서 허용할 HTTP 메서드 지정
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        // JSON 요청 및 CSRF 헤더 허용
        config.setAllowedHeaders(
                List.of("Content-Type", "X-XSRF-TOKEN")
        );

        // 브라우저가 인증 쿠키를 포함한 요청을 보낼 수 있도록 허용
        config.setAllowCredentials(true);

        // Preflight 결과를 브라우저가 최대 3,600초 동안 캐시하도록 지정
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 모든 요청 경로에 해당 CORS 정책 등록
        source.registerCorsConfiguration("/**", config);

        return source;
    }


}
