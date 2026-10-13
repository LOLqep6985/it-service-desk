package com.itservice.desk.model;

import lombok.Data;

@Data
public class Department {
    private Long id;
    private String name;
    private Campus campus;
    private String building;
}