package com.itservice.desk.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_order")
public class WorkOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private Long departmentId;
    private String departmentName;
    private Campus campus;
    private String location;
    private String contactPerson;
    private String contactPhone;
    private String priority;
    private WorkOrderStatus status;
    private Long assigneeId;
    private Long creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String repairRecord;
    private Long reportId;
    private SignStatus signStatus;
    private String signedBy;
    private LocalDateTime signedAt;
    private String signatureUrl;
}