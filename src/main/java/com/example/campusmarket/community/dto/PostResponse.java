package com.example.campusmarket.community.dto;

import java.util.List;

/**
 * 커뮤니티 게시글 응답 DTO
 *
 * @param id           Firestore 문서 ID
 * @param title        게시글 제목
 * @param content      게시글 내용
 * @param imageUrls    이미지 URL 목록
 * @param userid       작성자 uid
 * @param username     작성자 이름 (Firestore users 컬렉션에서 조회)
 * @param university   작성자 소속 대학교
 * @param viewCount    조회수
 * @param likeCount    좋아요 수
 * @param commentCount 댓글 수
 * @param createdAt    작성 시각 (Unix 밀리초)
 */
public record PostResponse(
    String id,
    String title,
    String content,
    List<String> imageUrls,
    String userid,
    String username,
    String university,
    Long viewCount,
    Long likeCount,
    Long commentCount,
    Long createdAt
) {}
