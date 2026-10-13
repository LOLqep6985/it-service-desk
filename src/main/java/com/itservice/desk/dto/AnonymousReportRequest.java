package com.itservice.desk.dto;

import lombok.Data;

@Data
public class AnonymousReportRequest {
    private String deviceCode;
    private String reporterName;
    private String deviceName;
    private String devicePhoto;
    private String problemDesc;
    private String departmentHead;
}