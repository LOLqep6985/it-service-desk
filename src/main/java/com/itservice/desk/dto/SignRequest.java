package com.itservice.desk.dto;

import lombok.Data;

@Data
public class SignRequest {
    private String deviceCode;
    private String signerName;
    private Boolean isDelegate;
    private String delegatedName;
    private String signatureUrl;
}