package com.media.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.media.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Slf4j
@RestControllerAdvice
@EnableKnife4j
@EnableSwagger2
public class WebExceptionAdvice {

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return Result.fail("服务器异常");
    }







}
