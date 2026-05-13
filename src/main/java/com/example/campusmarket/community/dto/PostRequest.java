package com.example.campusmarket.community.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 커뮤니티 게시글 작성 요청 DTO
 *
 * @param title     게시글 제목 (필수)
 * @param content   게시글 내용 (필수)
 * @param imageUrls 이미지 URL 목록 (선택, /api/images/upload 업로드 후 URL 전달)
 */
public record PostRequest(
    @NotBlank(message = "제목은 필수입니다.") String title,
    @NotBlank(message = "내용은 필수입니다.") String content,
    List<String> imageUrls
) {}
