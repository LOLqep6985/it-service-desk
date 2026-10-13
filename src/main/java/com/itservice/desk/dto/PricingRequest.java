package com.itservice.desk.dto;

import lombok.Data;

import java.util.List;

@Data
public class PricingRequest {
    private List<PricingItem> consumables;
}