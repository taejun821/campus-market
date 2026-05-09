package com.example.campusmarket.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "idToken은 필수입니다.") String idToken
) {}
