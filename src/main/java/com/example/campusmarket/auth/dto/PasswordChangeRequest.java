package com.example.campusmarket.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청 DTO
 *
 * @param currentPassword 현재 비밀번호 (본인 확인용)
 * @param newPassword     새 비밀번호 (최소 8자)
 */
public record PasswordChangeRequest(
    @NotBlank(message = "현재 비밀번호는 필수입니다.") String currentPassword,
    @NotBlank(message = "새 비밀번호는 필수입니다.") @Size(min = 8, message = "새 비밀번호는 최소 8자 이상이어야 합니다.") String newPassword
) {}
