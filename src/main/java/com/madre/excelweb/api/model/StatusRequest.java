package com.madre.excelweb.api.model;

import lombok.Data;

@Data
public class StatusRequest {
    private Long gameId;
    private String status;
}
