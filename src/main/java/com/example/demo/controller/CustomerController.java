package com.example.demo.controller;

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
import com.example.demo.dto.CustomerImportRequest;
import com.example.demo.dto.CustomerRequest;
import com.example.demo.dto.CustomerResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.CustomerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
@Validated
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> list(@RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Integer page,
                                                            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(customerService.list(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> findOne(@PathVariable Long id) {
        return ApiResponse.ok(customerService.findById(id));
    }

    @PostMapping
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok(customerService.create(request), "创建成功");
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody CustomerRequest req) {
        return ApiResponse.ok(customerService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/import")
    public ApiResponse<Integer> importBatch(@Valid @RequestBody CustomerImportRequest request) {
        int inserted = customerService.importBatch(request);
        return ApiResponse.ok(inserted, "导入完成");
    }
}
