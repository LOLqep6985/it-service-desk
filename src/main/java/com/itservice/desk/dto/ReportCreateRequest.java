package com.itservice.desk.dto;

import lombok.Data;

@Data
public class ReportCreateRequest {
    private Long departmentId;
    private String reporterName;
    private String deviceName;
    private String devicePhoto;
    private String problemDesc;
    private String departmentHead;
}