package com.hkywt.attendance.common.exception;

import com.hkywt.attendance.common.model.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBiz(BizException e) {
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream().findFirst().map(v -> v.getField() + ":" + v.getDefaultMessage()).orElse("参数校验失败");
        return ApiResult.fail(400, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraint(ConstraintViolationException e) {
        return ApiResult.fail(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleEx(Exception e) {
        log.error("Unhandled exception", e);
        return ApiResult.fail(500, "系统异常，请联系管理员");
    }
}
