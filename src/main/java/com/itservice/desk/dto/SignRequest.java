package com.itservice.desk.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 工单签字请求（待办一：参数校验加固）。
 *
 * <p>签字是报销的前提、也是审计留痕的关键动作，签名人不能为空，
 * 否则 {@code signed_by} 会写入空值，事后无法追责。
 */
@Data
public class SignRequest {

    /** 科室凭证（D3 修复后为科室身份的唯一可信来源） */
    private String deptToken;

    /** 已废弃：不再被采信，仅为兼容老客户端保留字段，防止反序列化失败 */
    private Long departmentId;

    /** 兼容旧方式：设备码 */
    private String deviceCode;

    @NotBlank(message = "签字人姓名不能为空")
    @Size(max = 50, message = "签字人姓名不能超过 50 个字符")
    private String signerName;

    private Boolean isDelegate;

    @Size(max = 50, message = "代签人姓名不能超过 50 个字符")
    private String delegatedName;

    @Size(max = 255, message = "签名图片地址不能超过 255 个字符")
    private String signatureUrl;

    /** 签字方式：HANDWRITTEN（默认）/ TYPED */
    @Size(max = 20, message = "签字方式取值过长")
    private String signType;
}
