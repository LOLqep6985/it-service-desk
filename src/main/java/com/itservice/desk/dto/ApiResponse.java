package com.itservice.desk.dto;

import lombok.Data;

@Data
public class ApiResponse {
    private int code;
    private String message;
    private Object data;

    public static ApiResponse ok(Object data) {
        ApiResponse r = new ApiResponse();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static ApiResponse error(String message) {
        return error(500, message);
    }

    public static ApiResponse error(int code, String message) {
        ApiResponse r = new ApiResponse();
        r.code = code;
        r.message = message;
        return r;
    }
}