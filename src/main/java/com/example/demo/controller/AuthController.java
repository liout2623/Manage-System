package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.CaptchaResponse;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.security.JwtProperties;
import com.example.demo.service.CaptchaService;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseCookie;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@SecurityRequirements
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final UserService userService;
    private final JwtProperties jwtProperties;
    private final CaptchaService captchaService;

    public AuthController(UserService userService, JwtProperties jwtProperties, CaptchaService captchaService) {
        this.userService = userService;
        this.jwtProperties = jwtProperties;
        this.captchaService = captchaService;
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        return ApiResponse.ok(captchaService.generate(), "验证码获取成功");
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        captchaService.validate(request.getCaptchaId(), request.getCaptchaCode());
        return ApiResponse.ok(userService.register(request), "注册成功");
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody AuthRequest request,
                                            HttpServletResponse response) {
        LoginResponse loginResponse = userService.login(request);
        addTokenCookie(response, loginResponse.getToken());
        // Token 不再返回到响应体，仅通过 HttpOnly Cookie 传递
        return ApiResponse.ok(loginResponse.getUser(), "登录成功");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        clearTokenCookie(response);
        return ApiResponse.ok(null, "已退出登录");
    }

    private void addTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = buildTokenCookie(token, jwtProperties.getExpireSeconds());
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = buildTokenCookie("", 0);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private ResponseCookie buildTokenCookie(String token, long maxAge) {
        return ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(jwtProperties.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
