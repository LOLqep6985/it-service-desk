package com.itservice.desk.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reimbursement_item")
public class ReimbursementItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reimbursementId;
    private Long workOrderId;
    private LocalDateTime createdAt;
}