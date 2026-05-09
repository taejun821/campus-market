package com.example.campusmarket.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank(message = "idToken은 필수입니다.") String idToken,
    @NotBlank(message = "이름은 필수입니다.") String name,
    @NotBlank(message = "학교명은 필수입니다.") String university
) {}
