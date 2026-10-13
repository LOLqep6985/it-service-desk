package com.itservice.desk.dto;

import lombok.Data;

import java.util.List;

@Data
public class RepairRequest {
    private String record;
    private List<ConsumableItem> consumables;
}