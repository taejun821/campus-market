package com.example.campusmarket.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO
 *
 * 이메일 + 비밀번호 조합으로 인증하며, 성공 시 JWT 토큰을 발급받는다.
 * - email    : 필수, 이메일 형식
 * - password : 필수 (평문 전달 후 BCrypt matches()로 저장된 해시와 비교)
 */
public record LoginRequest(
    @NotBlank(message = "이메일은 필수입니다.") @Email(message = "유효한 이메일 형식이어야 합니다.") String email,
    @NotBlank(message = "비밀번호는 필수입니다.") String password
) {}
