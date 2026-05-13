package com.example.campusmarket.community.dto;

/**
 * 커뮤니티 댓글 응답 DTO
 *
 * @param id        댓글 Firestore 문서 ID
 * @param postId    댓글이 달린 게시글 ID
 * @param content   댓글 내용
 * @param userid    작성자 uid
 * @param username  작성자 이름
 * @param createdAt 작성 시각 (Unix 밀리초, 작성 직후 null일 수 있음)
 */
public record CommentResponse(
    String id,
    String postId,
    String content,
    String userid,
    String username,
    Long createdAt
) {}
