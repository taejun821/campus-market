package com.example.campusmarket.trade.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 중고거래 게시물 수정 요청 DTO
 * null인 필드는 수정하지 않음 (부분 수정 허용)
 */
public record TradeUpdateRequest(
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.") String title,
    @Size(max = 2000, message = "설명은 2000자 이하여야 합니다.") String description,
    @PositiveOrZero(message = "가격은 0 이상이어야 합니다.") Long price,
    @Size(max = 200, message = "거래 위치는 200자 이하여야 합니다.") String location,
    @Size(max = 10, message = "이미지는 최대 10개까지 업로드할 수 있습니다.") List<String> imageUrls
) {}
