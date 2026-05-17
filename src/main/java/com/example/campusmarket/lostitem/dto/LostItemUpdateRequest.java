package com.example.campusmarket.lostitem.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 분실물 수정 요청 DTO
 * null인 필드는 수정하지 않음 (부분 수정 허용)
 */
public record LostItemUpdateRequest(
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.") String title,
    @Size(max = 2000, message = "설명은 2000자 이하여야 합니다.") String description,
    @Size(max = 200, message = "분실 장소는 200자 이하여야 합니다.") String location,
    String lostDate,
    @Size(max = 10, message = "이미지는 최대 10개까지 업로드할 수 있습니다.") List<String> imageUrls
) {}
