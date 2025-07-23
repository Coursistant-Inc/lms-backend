package com.coursistant.lms.exception;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.coursistant.lms.common.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 * Global Exception Handler
 */
@ControllerAdvice(basePackages="com.coursistant.lms.controller")
public class GlobalExceptionHandler {

    private static final Log log = LogFactory.get();

    /**
     * 统一异常处理
     * Unified exception handling
     * @ExceptionHandler, 主要用于捕获 Exception
     * Mainly used to catch Exception
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody // 返回 JSON 串
    // Returns JSON string
    public Result error(HttpServletRequest request, Exception e){
        log.error("异常信息：", e);
        // Exception message
        return Result.error();
    }

    /**
     * 处理自定义异常
     * Handle custom exceptions
     */
    @ExceptionHandler(CustomException.class)
    @ResponseBody // 返回 JSON 串
    // Returns JSON string
    public Result customError(HttpServletRequest request, CustomException e){
        return Result.error(e.getCode(), e.getMsg());
    }
}
