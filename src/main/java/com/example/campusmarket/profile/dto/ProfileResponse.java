package com.example.campusmarket.profile.dto;

/**
 * 프로필 응답 DTO
 *
 * - 내 프로필(GET /api/profile)       : email 포함
 * - 공개 프로필(GET /api/profile/{uid}) : email = null (개인정보 보호)
 *
 * @param uid        사용자 고유 ID
 * @param email      이메일 (공개 프로필 조회 시 null)
 * @param name       사용자 이름
 * @param university 소속 대학교
 * @param region     지역 (예: 서울 / 경기 / 인천)
 * @param createdAt  가입 시각 (Unix 밀리초)
 */
public record ProfileResponse(
    String uid,
    String email,
    String name,
    String university,
    String region,
    Long createdAt
) {}
