package com.itservice.desk.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrder {
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
}