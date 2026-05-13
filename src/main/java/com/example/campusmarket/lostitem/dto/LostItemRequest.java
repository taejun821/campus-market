package com.example.campusmarket.lostitem.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 분실물 등록 요청 DTO
 *
 * @param title       분실물 제목 (필수)
 * @param description 분실물 설명 (필수)
 * @param location    분실 장소 (필수)
 * @param lostDate    분실 날짜 (필수, 형식은 클라이언트 결정)
 * @param imageUrls   이미지 URL 목록 (선택, /api/images/upload 업로드 후 URL 전달)
 */
public record LostItemRequest(
    @NotBlank(message = "제목은 필수입니다.") String title,
    @NotBlank(message = "설명은 필수입니다.") String description,
    @NotBlank(message = "분실 장소는 필수입니다.") String location,
    @NotBlank(message = "분실 날짜는 필수입니다.") String lostDate,
    List<String> imageUrls
) {}
