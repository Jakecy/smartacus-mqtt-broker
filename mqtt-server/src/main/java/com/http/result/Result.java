package com.http.result;

public class Result<T> {

    private boolean success;
    private String message;
    private T content;
    private int code;

    public Result() {
    }

    public Result(boolean success, String message, T content, int code) {
        this.success = success;
        this.message = message;
        this.content = content;
        this.code = code;
    }

    public Result ok() {
        this.success = true;
        this.message = "ok";
        this.code = 200;
        return this;
    }

    public Result<T> ok(T t) {
        this.content = t;
        return this.ok();
    }

    public Result error(int code, String message, T content) {
        this.success = false;
        this.message = message;
        this.content = content;
        this.code = code;
        return this;
    }

    public Result error(int code, String message) {
        this.success = false;
        this.message = message;
        this.content = null;
        this.code = code;
        return this;
    }

    public Result error(String message) {
        this.success = false;
        this.message = message;
        this.code = 500;
        return this;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }

    public T getContent() {
        return this.content;
    }

    public int getCode() {
        return this.code;
    }
}
