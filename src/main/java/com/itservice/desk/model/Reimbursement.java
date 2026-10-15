package com.itservice.desk.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    private LocalDateTime reimbursedAt;
    /** 科室签字的手写签名图片 URL（报销闭环留痕） */
    private String signatureUrl;
    /** 签字方式：HANDWRITTEN（手写）/ TYPED（输入姓名生成） */
    private SignType signType;
    private LocalDateTime createdAt;

    /** 非表字段：用于列表展示科室名称 */
    @TableField(exist = false)
    private String departmentName;
}