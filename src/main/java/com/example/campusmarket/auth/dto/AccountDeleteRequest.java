package com.example.campusmarket.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 회원 탈퇴 요청 DTO
 *
 * @param password 현재 비밀번호 (본인 확인용)
 */
public record AccountDeleteRequest(
    @NotBlank(message = "비밀번호는 필수입니다.") String password
) {}
