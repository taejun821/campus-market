package com.example.campusmarket.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO
 *
 * 검증 규칙:
 * - university : 필수 (대학교명)
 * - email      : 필수, 이메일 형식 (서비스 레이어에서 허용 도메인 추가 검증)
 * - name       : 필수 (사용자 이름)
 * - password   : 필수, 최소 8자 이상 (저장 시 BCrypt 해시 처리)
 */
public record RegisterRequest(
    @NotBlank(message = "대학교명은 필수입니다.") String university,
    @NotBlank(message = "이메일은 필수입니다.") @Email(message = "유효한 이메일 형식이어야 합니다.") String email,
    @NotBlank(message = "이름은 필수입니다.") String name,
    @NotBlank(message = "비밀번호는 필수입니다.") @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.") String password
) {}
