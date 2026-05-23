package com.example.demo.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class TherapistServiceRequest {

    @NotNull(message = "理疗师ID不能为空")
    private Long therapistId;

    @NotEmpty(message = "服务项目ID列表不能为空")
    private List<Long> serviceIds;

    public Long getTherapistId() {
        return therapistId;
    }

    public void setTherapistId(Long therapistId) {
        this.therapistId = therapistId;
    }

    public List<Long> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(List<Long> serviceIds) {
        this.serviceIds = serviceIds;
    }
}
