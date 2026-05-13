package com.example.campusmarket.lostitem.dto;

/**
 * 좋아요 토글 결과 응답 DTO
 *
 * @param liked     토글 후 현재 좋아요 상태 (true: 좋아요 추가됨, false: 취소됨)
 * @param likeCount 토글 후 총 좋아요 수
 */
public record LikeResponse(boolean liked, long likeCount) {}
