package com.firstpages.global.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.firstpages.global.response.code.CommonSuccessCode;
import com.firstpages.global.response.code.ErrorResponseCode;
import com.firstpages.global.response.code.SuccessResponseCode;
import lombok.Getter;

@Getter
@JsonPropertyOrder({
        "success",
        "code",
        "message",
        "data"
})
public class ApiResponse<T> {

    private final boolean success;

    private final String code;

    private final String message;

    private final T data;

    // 외부에서 직접 생성하지 못하도록 생성자를 제한합니다.
    private ApiResponse(
            boolean success,
            String code,
            String message,
            T data
    ) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 데이터를 포함한 성공 응답을 생성합니다.
    public static <T> ApiResponse<T> success(
            SuccessResponseCode successCode,
            T data
    ) {
        return new ApiResponse<>(
                true,
                successCode.getCode(),
                successCode.getMessage(),
                data
        );
    }

    // 데이터가 없는 성공 응답을 생성합니다.
    public static <T> ApiResponse<T> success(
            SuccessResponseCode successCode
    ) {
        return new ApiResponse<>(
                true,
                successCode.getCode(),
                successCode.getMessage(),
                null
        );
    }

    // 기본 메시지를 사용하는 실패 응답을 생성합니다.
    public static ApiResponse<Void> failure(
            ErrorResponseCode errorCode
    ) {
        return new ApiResponse<>(
                false,
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }

    // 별도의 메시지를 사용하는 실패 응답을 생성합니다
    public static ApiResponse<Void> failure(
            ErrorResponseCode errorCode,
            String message
    ) {
        return new ApiResponse<>(
                false,
                errorCode.getCode(),
                message,
                null
        );
    }
}
