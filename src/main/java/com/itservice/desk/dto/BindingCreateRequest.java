package com.itservice.desk.dto;

import lombok.Data;

@Data
public class BindingCreateRequest {
    private Long departmentId;
    private String deviceCode;
}