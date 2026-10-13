package com.itservice.desk.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("consumable")
public class Consumable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workOrderId;
    private String name;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Boolean isIou;
    private String iouNo;
}