package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class RegisterRequest extends AuthRequest {
    @NotBlank
    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
