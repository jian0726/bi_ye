package com.jianyou.blog.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：任何异常都以统一 Result 结构返回
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("参数不合法");
        return Result.error(400, message);
    }

    /**
     * 访问了不存在的接口/静态资源：返回真正的 404，
     * 而不是被兜底成 500（否则「路由不存在」会被误读成服务端故障）。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        log.warn("接口不存在: {}", e.getResourcePath());
        return Result.error(404, "接口不存在");
    }

    /** 上传接口缺 file 部分/非 multipart 请求：按参数错误返回，而不是兜底 500 */
    @ExceptionHandler(MultipartException.class)
    public Result<Void> handleMultipart(MultipartException e) {
        log.warn("上传请求不合法: {}", e.getMessage());
        return Result.error(400, "请选择要上传的文件");
    }

    /**
     * 路径变量/查询参数类型不匹配（如 /portal/articles/hot、?page=abc）：
     * 按参数错误返回 400，而不是兜底成 500（否则「参数写错」会被误读成服务端故障）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: name={} value={}", e.getName(), e.getValue());
        return Result.error(400, "请求参数格式不正确");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未处理异常", e);
        return Result.error(500, "服务器开小差了，请稍后再试");
    }
}
