package com.itservice.desk.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PricingItem {
    private Long id;
    private BigDecimal unitPrice;
}