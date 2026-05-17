package com.example.campusmarket.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 중고거래 게시물 등록 요청 DTO
 *
 * @param title       게시물 제목 (필수)
 * @param description 상품 설명 (필수)
 * @param price       가격 (필수, 0 이상의 정수, 무료 나눔 시 0)
 * @param location    거래 희망 장소 (필수)
 * @param category    카테고리 (필수: 전자기기 / 의류 / 도서 / 생활용품 / 식품 / 스포츠 / 기타)
 * @param imageUrls   이미지 URL 목록 (선택, /api/images/upload 업로드 후 URL 전달)
 */
public record TradeRequest(
    @NotBlank(message = "제목은 필수입니다.") @Size(max = 100, message = "제목은 100자 이하여야 합니다.") String title,
    @NotBlank(message = "설명은 필수입니다.") @Size(max = 2000, message = "설명은 2000자 이하여야 합니다.") String description,
    @NotNull(message = "가격은 필수입니다.") @PositiveOrZero(message = "가격은 0 이상이어야 합니다.") Long price,
    @NotBlank(message = "거래 위치는 필수입니다.") @Size(max = 200, message = "거래 위치는 200자 이하여야 합니다.") String location,
    @NotBlank(message = "카테고리는 필수입니다.") String category,
    @Size(max = 10, message = "이미지는 최대 10개까지 업로드할 수 있습니다.") List<String> imageUrls
) {}
