package com.itservice.desk.dto;

import lombok.Data;

@Data
public class ReimbursementSignRequest {
    /** 新方式：直接指定科室 */
    private Long departmentId;
    /** 兼容旧方式：设备码 */
    private String deviceCode;
    private String signerName;
    private String signatureUrl;
}