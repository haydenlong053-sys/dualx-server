package com.app.common.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一返回结果
 */
public class Result<T> {
    
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    // ========== 成功响应 ==========
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }
    
    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    // ========== 失败响应 ==========
    public static <T> Result<T> failure() {
        return new Result<>(500, "error", null);
    }
    
    public static <T> Result<T> failure(String message) {
        return new Result<>(500, message, null);
    }
    
    public static <T> Result<T> failure(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    // ========== Getter/Setter ==========
    public Integer getCode() { return code; }
    public Result<T> setCode(Integer code) { this.code = code; return this; }
    
    public String getMessage() { return message; }
    public Result<T> setMessage(String message) { this.message = message; return this; }
    
    public T getData() { return data; }
    public Result<T> setData(T data) { this.data = data; return this; }
    
    public Long getTimestamp() { return timestamp; }
    
    // 兼容旧代码
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(4);
        map.put("success", isSuccess());
        map.put("code", code);
        map.put("msg", message);
        map.put("data", data);
        return map;
    }
    
    public boolean isSuccess() {
        return code != null && code == 200;
    }
}