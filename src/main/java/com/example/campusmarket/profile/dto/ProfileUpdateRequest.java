package com.example.campusmarket.profile.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 프로필 수정 요청 DTO
 * 현재 이름(name)만 변경 가능하다.
 */
public record ProfileUpdateRequest(
    @NotBlank(message = "이름은 필수입니다.") String name
) {}
