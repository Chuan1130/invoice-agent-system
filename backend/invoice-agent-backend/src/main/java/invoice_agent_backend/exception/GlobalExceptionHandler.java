package invoice_agent_backend.exception;

import invoice_agent_backend.common.ApiResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/*
 ** 全局异常处理器。
 ** 作用：
 ** Controller / Service / Mapper 过程中如果抛出异常，
 ** 这里会统一把异常包装成 ApiResponse 格式返回给前端。
 **
 ** 这样前端不需要解析 Spring Boot 默认的错误页面或复杂 JSON，
 ** 只需要统一看 code / message / data。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e) {
        return ApiResponse.fail(e.getMessage());
    }

    @ExceptionHandler(MultipartException.class)
    public ApiResponse<Void> handleMultipartException(MultipartException e) {
        return ApiResponse.fail("文件上传请求格式错误，请使用 form-data，并确保文件字段 key 为 file");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return ApiResponse.fail("上传文件过大，最大支持 10MB");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return ApiResponse.fail("缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        return ApiResponse.fail("系统异常：" + e.getMessage());
    }
}