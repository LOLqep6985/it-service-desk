package com.itservice.desk.dto;

import lombok.Data;

@Data
public class AnonymousReportRequest {
    /** 新方式：直接指定科室 */
    private Long departmentId;
    /** 兼容旧方式：设备码（已废弃，保留兼容） */
    private String deviceCode;
    private String reporterName;
    private String deviceName;
    private String problemDesc;
    private String departmentHead;
}