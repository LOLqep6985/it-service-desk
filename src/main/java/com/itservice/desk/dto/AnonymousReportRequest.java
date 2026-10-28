package com.itservice.desk.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 科室端免登录报修请求（待办一：参数校验加固）。
 *
 * <p>修复前：DTO 无任何校验注解，Controller 也未校验，
 * 实测传空的 {@code reporterName}/{@code deviceName} 仍能创建成功并落库空串，
 * 数据库只有 {@code NOT NULL} 约束（空串不等于 NULL，拦不住）。
 *
 * <p>修复后：必填项用 {@link NotBlank}（同时拦 null、空串、纯空白），
 * 长度用 {@link Size} 与列宽对齐，避免超长导致 SQL 报错。
 */
@Data
public class AnonymousReportRequest {

    /** 科室凭证（D3 修复后为科室身份的唯一可信来源） */
    private String deptToken;

    /** 已废弃：不再被采信，仅为兼容老客户端保留字段 */
    private Long departmentId;

    /** 兼容旧方式：设备码（已废弃，保留兼容） */
    private String deviceCode;

    @NotBlank(message = "报修人姓名不能为空")
    @Size(max = 50, message = "报修人姓名不能超过 50 个字符")
    private String reporterName;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称不能超过 100 个字符")
    private String deviceName;

    @Size(max = 500, message = "问题描述不能超过 500 个字符")
    private String problemDesc;

    @Size(max = 50, message = "科室负责人姓名不能超过 50 个字符")
    private String departmentHead;

    /** 科室端设备标识（首次使用由前端生成并本地保存），用于定向投递待签字消息 */
    @Size(max = 64, message = "设备标识长度不能超过 64 个字符")
    private String deviceId;
}
