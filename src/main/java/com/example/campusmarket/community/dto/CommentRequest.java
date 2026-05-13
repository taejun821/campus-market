package com.example.campusmarket.community.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 커뮤니티 댓글 작성 요청 DTO
 * 댓글 내용(content)만 전달하며, 작성자 정보는 JWT 토큰에서 추출한다.
 */
public record CommentRequest(
    @NotBlank(message = "내용은 필수입니다.") String content
) {}
