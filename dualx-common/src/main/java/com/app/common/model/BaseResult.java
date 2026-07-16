package com.app.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BaseResult<T> {
    private int code = 0;
    private String msg = "Success";
    private long timeMillis = System.currentTimeMillis();
    private T data;

    public BaseResult() {
    }

    public BaseResult(T data) {
        this.data = data;
    }

    public BaseResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public BaseResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static BaseResult<?> success(String msg) {
        return new BaseResult<>(0, msg);
    }

    public static BaseResult<?> success() {
        return new BaseResult<>(0, "Success");
    }

    public static BaseResult<?> error(String msg) {
        return new BaseResult<>(400, msg);
    }

    public static BaseResult<?> error() {
        return new BaseResult<>(400, "Failed");
    }

}
