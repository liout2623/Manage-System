package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class AppointmentStatusRequest {

    @NotNull(message = "状态不能为空")
    @Pattern(regexp = "COMPLETED|CANCELLED", message = "仅支持变更为 COMPLETED 或 CANCELLED")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
