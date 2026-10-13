package com.itservice.desk.dto;

import lombok.Data;

@Data
public class ReimbursementSignRequest {
    private String deviceCode;
    private String signerName;
    private String signatureUrl;
}