package com.itservice.desk.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("reimbursement")
public class Reimbursement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String month;
    private Long departmentId;
    private BigDecimal totalAmount;
    private ReimbursementStatus status;
    private String signedBy;
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;
}