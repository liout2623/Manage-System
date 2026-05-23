package com.example.demo.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ServiceResponse;
import com.example.demo.dto.TherapistServiceRequest;
import com.example.demo.service.TherapistServiceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/therapist-services")
@Validated
public class TherapistServiceController {

    private final TherapistServiceService therapistServiceService;

    public TherapistServiceController(TherapistServiceService therapistServiceService) {
        this.therapistServiceService = therapistServiceService;
    }

    /**
     * 查询某理疗师负责的项目列表（所有登录用户可调用）
     */
    @GetMapping("/{therapistId}")
    public ApiResponse<List<ServiceResponse>> findByTherapistId(@PathVariable Long therapistId) {
        return ApiResponse.ok(therapistServiceService.findByTherapistId(therapistId));
    }

    /**
     * 管理员为理疗师批量分配服务项目（仅 ADMIN）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ServiceResponse>> assignServices(@Valid @RequestBody TherapistServiceRequest request) {
        return ApiResponse.ok(therapistServiceService.assignServices(request), "分配成功");
    }

    /**
     * 移除某条关联（仅 ADMIN）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteById(@PathVariable Long id) {
        therapistServiceService.deleteById(id);
        return ApiResponse.ok(null);
    }
}
