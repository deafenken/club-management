package com.club.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 统一返回封装
 * 所有接口使用此类封装响应，前端axios拦截器统一解析
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;       // 状态码: 200成功 4xx客户端错误 5xx服务端错误
    private String msg;     // 提示信息
    private T data;         // 数据体

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(400, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }
}
