package com.firstpages.global.security;

import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.code.ErrorResponseCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(
            HttpServletResponse response,
            ErrorResponseCode errorCode
    ) throws IOException {
        // HTTP 응답 헤더 설정
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // HTTP 응답 본문 설정
        ApiResponse<Void> body = ApiResponse.failure(errorCode);

        // objectMapper를 통해 body를 역직렬화 후 response에 작성
        objectMapper.writeValue(response.getWriter(), body);
    }
}
