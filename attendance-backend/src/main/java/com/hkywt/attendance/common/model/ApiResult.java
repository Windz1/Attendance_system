package com.hkywt.attendance.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder().code(0).message("success").data(data).build();
    }

    public static ApiResult<Void> success() {
        return ApiResult.<Void>builder().code(0).message("success").build();
    }

    public static ApiResult<Void> fail(Integer code, String message) {
        return ApiResult.<Void>builder().code(code).message(message).build();
    }
}
