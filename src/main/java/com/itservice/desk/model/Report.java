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
    private String devicePhoto;
    private String problemDesc;
    private String departmentHead;
    private ReportStatus status;
    private Long creatorId;
    private Long bindingId;
    private LocalDateTime createdAt;
}