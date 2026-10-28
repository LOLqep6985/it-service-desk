package com.itservice.desk.dto;

import lombok.Data;

@Data
public class ReimbursementSignRequest {
    /** 科室凭证（D3 修复后为科室身份的唯一可信来源） */
    private String deptToken;
    /** 已废弃：不再被采信，仅为兼容老客户端保留字段，防止反序列化失败 */
    private Long departmentId;
    /** 兼容旧方式：设备码 */
    private String deviceCode;
    private String signerName;
    private String signatureUrl;
    /** 签字方式：HANDWRITTEN（默认）/ TYPED */
    private String signType;
}