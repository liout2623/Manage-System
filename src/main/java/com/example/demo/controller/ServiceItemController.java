package com.example.demo.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.ServiceRequest;
import com.example.demo.dto.ServiceResponse;
import com.example.demo.service.ServiceItemService;

@RestController
@RequestMapping("/api/services")
@Validated
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    public ServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ServiceResponse>> list(@RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size,
                                                           @RequestParam(required = false) String sort,
                                                           @RequestParam(required = false) Boolean active) {
        return ApiResponse.ok(serviceItemService.list(keyword, page, size, sort, active));
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceResponse> findOne(@PathVariable Long id) {
        return ApiResponse.ok(serviceItemService.findById(id));
    }

    @PostMapping
    public ApiResponse<ServiceResponse> create(@Validated(ServiceRequest.OnCreate.class) @RequestBody ServiceRequest request) {
        return ApiResponse.ok(serviceItemService.create(request), "创建成功");
    }

    @PutMapping("/{id}")
    public ApiResponse<ServiceResponse> update(@PathVariable Long id,
                                               @Validated(ServiceRequest.OnUpdate.class) @RequestBody ServiceRequest request) {
        return ApiResponse.ok(serviceItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        serviceItemService.delete(id);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/toggle")
    public ApiResponse<ServiceResponse> toggle(@PathVariable Long id) {
        return ApiResponse.ok(serviceItemService.toggle(id), "状态已切换");
    }
}