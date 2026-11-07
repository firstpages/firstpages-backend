package com.firstpages.global.config;

import com.firstpages.global.security.SecurityErrorResponseWriter;
import com.firstpages.global.security.jwt.JwtAccessDeniedHandler;
import com.firstpages.global.security.jwt.JwtAuthenticationEntryPoint;
import com.firstpages.global.security.jwt.JwtAuthenticationFilter;
import com.firstpages.global.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
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
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/oauth2/**",
                                "/login/**"
                        )
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        )
                        .permitAll()
                        .anyRequest()
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

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}")
            List<String> allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(allowedOrigins);

        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        config.setAllowedHeaders(
                List.of("Authorization", "Content-Type")
        );

        config.setAllowCredentials(false);

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }


}
