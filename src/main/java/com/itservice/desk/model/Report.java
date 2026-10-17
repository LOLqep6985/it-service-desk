package com.itservice.desk.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reportNo;
    private Long departmentId;
    private String departmentName;
    private String reporterName;
    private String deviceName;
    private String problemDesc;
    private String departmentHead;
    private ReportStatus status;
    private Long creatorId;
    private Long bindingId;
    /** 提交该报修的科室端设备标识（用于把待签字消息只投给报修人那台手机） */
    private String clientDeviceId;
    private LocalDateTime createdAt;
}