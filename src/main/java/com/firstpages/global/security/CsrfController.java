package com.firstpages.global.security;


import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.ResponseFactory;
import com.firstpages.global.response.code.CommonSuccessCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 클라이언트에서 CSRF 토큰을 초기화하기 위한 컨트롤러
@RestController
public class CsrfController {

    // CSRF 토큰을 초기화
    @GetMapping("/api/csrf")
    public ResponseEntity<ApiResponse<Void>> csrf(CsrfToken csrfToken) {

        // 지연 로딩된 CSRF 토큰을 실제로 생성하거나 조회
        csrfToken.getToken();

        return ResponseFactory.success(CommonSuccessCode.OK);
    }
}
