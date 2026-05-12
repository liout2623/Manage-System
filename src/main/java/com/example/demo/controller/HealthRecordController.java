package com.example.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.RecordRequest;
import com.example.demo.dto.RecordResponse;
import com.example.demo.service.HealthRecordService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers/{customerId}/records")
@Validated
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    public HealthRecordController(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @GetMapping
    public ApiResponse<PageResponse<RecordResponse>> list(@PathVariable Long customerId,
                                                          @RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(healthRecordService.listByCustomer(customerId, page, size));
    }

    @PostMapping
    public ApiResponse<RecordResponse> create(@PathVariable Long customerId,
                                              @Valid @RequestBody RecordRequest request,
                                              Authentication authentication) {
        return ApiResponse.ok(
                healthRecordService.create(customerId, request, authentication.getName()),
                "创建成功"
        );
    }

    @PutMapping("/{recordId}")
    public ApiResponse<RecordResponse> update(@PathVariable Long customerId,
                                              @PathVariable Long recordId,
                                              @Valid @RequestBody RecordRequest request,
                                              Authentication authentication) {
        return ApiResponse.ok(healthRecordService.update(customerId, recordId, request, authentication.getName()));
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> delete(@PathVariable Long customerId,
                                    @PathVariable Long recordId,
                                    Authentication authentication) {
        healthRecordService.delete(customerId, recordId, authentication.getName());
        return ApiResponse.ok(null);
    }
}
