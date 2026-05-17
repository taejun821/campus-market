package com.example.campusmarket.profile.dto;

import jakarta.validation.constraints.Size;

/**
 * 프로필 수정 요청 DTO
 * null인 필드는 수정하지 않음 (부분 수정 허용)
 *
 * @param name       변경할 이름 (선택)
 * @param university 변경할 대학교명 (선택)
 * @param region     변경할 지역 (선택, 예: 서울 / 경기 / 인천 등)
 */
public record ProfileUpdateRequest(
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.") String name,
    @Size(max = 100, message = "대학교명은 100자 이하여야 합니다.") String university,
    @Size(max = 30, message = "지역은 30자 이하여야 합니다.") String region
) {}
