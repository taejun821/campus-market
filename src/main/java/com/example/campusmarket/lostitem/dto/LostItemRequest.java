package com.example.campusmarket.lostitem.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record LostItemRequest(
    @NotBlank(message = "제목은 필수입니다.") String title,
    @NotBlank(message = "설명은 필수입니다.") String description,
    @NotBlank(message = "분실 장소는 필수입니다.") String location,
    @NotBlank(message = "분실 날짜는 필수입니다.") String lostDate,
    List<String> imageUrls
) {}
