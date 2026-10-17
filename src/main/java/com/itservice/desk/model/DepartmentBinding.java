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
    /** 该设备是否为本科室负责人（护士长）设备：待签字消息按它定向投递 */
    private Boolean isHead;
}