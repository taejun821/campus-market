package com.example.campusmarket.lostitem.dto;

import java.util.List;

/**
 * 분실물 응답 DTO
 *
 * @param id          Firestore 문서 ID
 * @param title       분실물 제목
 * @param description 분실물 설명
 * @param location    분실 장소
 * @param lostDate    분실 날짜
 * @param imageUrls   이미지 URL 목록
 * @param status      분실물 상태 (LOST: 분실중 / FOUND: 찾았음)
 * @param userid      등록자 uid
 * @param createdAt   등록 시각 (Unix 밀리초, 등록 직후 null일 수 있음)
 * @param viewCount   조회수 (단건 조회 시마다 증가)
 * @param likeCount   좋아요 수
 */
public record LostItemResponse(
    String id,
    String title,
    String description,
    String location,
    String lostDate,
    List<String> imageUrls,
    String status,
    String userid,
    Long createdAt,
    Long viewCount,
    Long likeCount
) {}
