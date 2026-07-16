package com.app.common.exception;

import com.app.common.enums.BaseResultCodeEnum;
import com.app.common.model.BaseResult;
import com.app.common.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    @ResponseBody
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResult<?>> handleException(Exception e) {
        log.error(">>> Exception 请处理: {}", e.getLocalizedMessage(),e);
        String message = MessageUtil.get(BaseResultCodeEnum.valueOf(500).i18nKey());
        BaseResult<?> result = new BaseResult<>(500, message, null);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ResponseBody
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResult<?>> handleRuntimeException(RuntimeException e) {
        log.error(">>> RuntimeException: {}", e.getLocalizedMessage(),e);
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            if (stackTraceElement.getClassName().startsWith("com.app") && stackTraceElement.getLineNumber() > 0) {
                log.error("错误位置在：{}.{}(), {}行",
                    stackTraceElement.getClassName(),
                    stackTraceElement.getMethodName(),
                    stackTraceElement.getLineNumber());
            }
        }
        BaseResultCodeEnum enumValue = BaseResultCodeEnum.valueOf(500);
        String message = null;
        if(enumValue != null){
            message = MessageUtil.get(enumValue.i18nKey());
        }
        BaseResult<?> result = new BaseResult<>(500, message, null);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ResponseBody
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResult<?>> handleException(IllegalArgumentException e) {
        log.error(">>> IllegalArgumentException: {}", e.getLocalizedMessage(),e);
        String message = MessageUtil.get(BaseResultCodeEnum.valueOf(400).i18nKey());
        BaseResult<?> result = new BaseResult<>(400, message, null);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ResponseBody
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResult<?>> handleException(HttpMessageNotReadableException e) {
        log.error(">>> HttpMessageNotReadableException: {}", e.getLocalizedMessage(),e);
        String message = MessageUtil.get(BaseResultCodeEnum.valueOf(400).i18nKey());
        BaseResult<?> result = new BaseResult<>(400, message, null);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResult<?>> handleException(MethodArgumentNotValidException e) {
        log.error(">>> MethodArgumentNotValidException: {}", e.getLocalizedMessage());
        String message = MessageUtil.get(BaseResultCodeEnum.valueOf(400).i18nKey());
        BaseResult<?> result = new BaseResult<>(400, message, null);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ResponseBody
    @ExceptionHandler(DcException.class)
    public ResponseEntity<BaseResult<?>> handleException(DcException e) {
        log.error(">>> DcException: {}", e.getLocalizedMessage());
        String message = MessageUtil.get(e.getResultCodeEnum().i18nKey());
        BaseResult<?> result = new BaseResult<>(e.getCode(), message, null);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
