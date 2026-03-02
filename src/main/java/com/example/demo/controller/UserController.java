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
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.dto.UserUpsertRequest;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String role,
                                                        @RequestParam(required = false) Boolean active,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(userService.list(keyword, role, active, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findOne(@PathVariable Long id) {
        return ApiResponse.ok(userService.findById(id));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserUpsertRequest request) {
        return ApiResponse.ok(userService.create(request), "创建成功");
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody UserUpsertRequest req) {
        return ApiResponse.ok(userService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok(null);
    }
}
