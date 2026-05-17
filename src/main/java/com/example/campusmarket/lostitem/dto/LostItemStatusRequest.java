package com.example.campusmarket.lostitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 분실물 상태 변경 요청 DTO
 * LOST: 분실중 / FOUND: 찾았음
 */
public record LostItemStatusRequest(
    @NotBlank(message = "상태값은 필수입니다.")
    @Pattern(regexp = "LOST|FOUND", message = "status는 LOST 또는 FOUND여야 합니다.")
    String status
) {}
