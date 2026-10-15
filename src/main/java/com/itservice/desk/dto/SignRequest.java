package com.itservice.desk.dto;

import lombok.Data;

@Data
public class SignRequest {
    /** 新方式：直接指定科室 */
    private Long departmentId;
    /** 兼容旧方式：设备码 */
    private String deviceCode;
    private String signerName;
    private Boolean isDelegate;
    private String delegatedName;
    private String signatureUrl;
    /** 签字方式：HANDWRITTEN（默认）/ TYPED */
    private String signType;
}