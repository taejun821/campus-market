package com.example.campusmarket.auth.dto;

/**
 * 회원가입/로그인 성공 응답 DTO
 *
 * Flutter 클라이언트는 token을 로컬에 저장하고,
 * 이후 모든 인증 필요 요청의 Authorization 헤더에 "Bearer {token}"으로 전송한다.
 *
 * @param uid        사용자 고유 ID (UUID)
 * @param email      이메일 주소
 * @param name       사용자 이름
 * @param university 소속 대학교
 * @param region     사용자 지역 (예: 서울 / 경기 / 인천) — 지역별 거래 필터링 기준
 * @param createdAt  가입 시각 (Unix 밀리초, 회원가입 직후에는 null일 수 있음)
 * @param token      발급된 JWT 토큰
 */
public record UserResponse(
    String uid,
    String email,
    String name,
    String university,
    String region,
    Long createdAt,
    String token
) {}
