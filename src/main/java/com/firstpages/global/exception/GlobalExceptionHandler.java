package com.firstpages.global.exception;

import com.firstpages.global.response.ApiResponse;
import com.firstpages.global.response.ResponseFactory;
import com.firstpages.global.response.code.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 예외 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException e
    ) {
        log.warn("Business exception: code={}",
                e.getErrorCode().getCode());

        return ResponseFactory.failure(
                e.getErrorCode(),
                e.getMessage()
        );
    }

    // @Valid 요청 DTO 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e
    ) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(CommonErrorCode.INVALID_INPUT.getMessage());

        log.warn("Validation failed: count={}",
                e.getBindingResult().getErrorCount());

        return ResponseFactory.failure(
                CommonErrorCode.INVALID_INPUT,
                message
        );
    }

    // 잘못된 JSON 요청 본문
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequestBody(
            HttpMessageNotReadableException e
    ) {
        log.warn("Invalid request body");

        return ResponseFactory.failure(
                CommonErrorCode.INVALID_REQUEST_BODY
        );
    }

    // 요청 파라미터 타입 불일치
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e
    ) {
        log.warn("Parameter type mismatch: name={}", e.getName());

        return ResponseFactory.failure(
                CommonErrorCode.INVALID_PARAMETER
        );
    }

    // 필수 요청 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException e
    ) {
        log.warn("Missing parameter: name={}",
                e.getParameterName());

        return ResponseFactory.failure(
                CommonErrorCode.MISSING_PARAMETER
        );
    }

    // 예상하지 못한 서버 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception e
    ) {
        log.error("Unexpected server exception", e);

        return ResponseFactory.failure(
                CommonErrorCode.INTERNAL_SERVER_ERROR
        );
    }
}
