package com.itservice.desk.dto;

import lombok.Data;

@Data
public class WorkOrderCreateRequest {
    private String title;
    private String description;
    private Long departmentId;
    private String location;
    private String contactPerson;
    private String contactPhone;
    private String priority;
}