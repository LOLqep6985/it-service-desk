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
        ApiResponse r = new ApiResponse();
        r.code = 500;
        r.message = message;
        return r;
    }
}