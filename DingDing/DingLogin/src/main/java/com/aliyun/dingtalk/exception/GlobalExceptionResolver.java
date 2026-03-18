package com.aliyun.dingtalk.exception;

import com.aliyun.dingtalk.model.ServiceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionResolver {

    // 从配置文件读取环境标识（application.yml中配置：spring.profiles.active=dev/prod）
    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    /**
     * 处理所有不可知异常（兜底）
     *
     * @param e 异常
     * @return json结果
     */
    @ExceptionHandler(Exception.class)
    public ServiceResult handleException(Exception e) {
        // 优化日志：固定标题+完整堆栈，避免e.getMessage()为null的情况
        log.error("【全局异常捕获-未知异常】", e);
        
        // 开发环境返回具体异常信息，生产环境返回通用提示（核心修改）
        String errorMsg = "prod".equals(activeProfile) 
                ? HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()  // 生产：通用提示
                : e.getMessage() != null ? e.getMessage() : e.toString(); // 开发：具体异常信息
        
        return ServiceResult.getFailureResult(
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), 
                errorMsg
        );
    }

    /**
     * 处理钉钉API调用自定义异常
     *
     * @param e 异常
     * @return json结果
     */
    @ExceptionHandler(InvokeDingTalkException.class)
    public ServiceResult handleInvokeDingTalkException(InvokeDingTalkException e) {
        log.error("【全局异常捕获-钉钉API调用异常】", e);
        return ServiceResult.getFailureResult(e.getErrCode(), e.getErrMsg());
    }

    // ============ 新增：处理参数相关异常（返回400，符合HTTP规范） ============
    /**
     * 处理请求参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ServiceResult handleMissingParamException(MissingServletRequestParameterException e) {
        log.error("【全局异常捕获-参数缺失】", e);
        String errorMsg = "请求参数缺失：" + e.getParameterName();
        return ServiceResult.getFailureResult(String.valueOf(HttpStatus.BAD_REQUEST.value()), errorMsg);
    }

    /**
     * 处理请求参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ServiceResult handleParamTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("【全局异常捕获-参数类型不匹配】", e);
        String errorMsg = String.format("参数类型不匹配：参数%s期望类型%s，实际传入%s",
                e.getName(), e.getRequiredType().getSimpleName(), e.getValue());
        return ServiceResult.getFailureResult(String.valueOf(HttpStatus.BAD_REQUEST.value()), errorMsg);
    }

    /**
     * 处理@Valid参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ServiceResult handleValidException(Exception e) {
        log.error("【全局异常捕获-参数校验失败】", e);
        // 提取校验失败的字段和提示
        String errorMsg;
        if (e instanceof MethodArgumentNotValidException) {
            errorMsg = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("；"));
        } else {
            errorMsg = ((BindException) e).getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("；"));
        }
        return ServiceResult.getFailureResult(String.valueOf(HttpStatus.BAD_REQUEST.value()), errorMsg);
    }
}