package com.example.demo.controller;

import java.time.LocalDate;

import org.springframework.security.access.prepost.PreAuthorize;
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
import com.example.demo.dto.AppointmentRequest;
import com.example.demo.dto.AppointmentResponse;
import com.example.demo.dto.AppointmentStatusRequest;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.AppointmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointments")
@Validated
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AppointmentResponse>> list(
            @RequestParam(required = false) Long therapistId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(appointmentService.list(therapistId, customerId, date, startDate, endDate, status, page, size, sort));
    }

    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(appointmentService.findById(id));
    }

    @PostMapping
    public ApiResponse<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest request) {
        return ApiResponse.ok(appointmentService.create(request), "创建成功");
    }

    @PutMapping("/{id}")
    public ApiResponse<AppointmentResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody AppointmentRequest request) {
        return ApiResponse.ok(appointmentService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AppointmentResponse> changeStatus(@PathVariable Long id,
                                                          @Valid @RequestBody AppointmentStatusRequest request) {
        return ApiResponse.ok(appointmentService.changeStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        appointmentService.delete(id);
        return ApiResponse.ok(null);
    }
}
