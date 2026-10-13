package com.itservice.desk.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("department_binding")
public class DepartmentBinding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long departmentId;
    private String deviceCode;
    private LocalDateTime bindAt;
    private Boolean active;
}