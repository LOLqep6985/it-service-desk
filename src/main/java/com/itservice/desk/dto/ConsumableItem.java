package com.itservice.desk.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConsumableItem {
    private String name;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Boolean isIou;
    private String iouNo;
}