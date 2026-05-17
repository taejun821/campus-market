package com.example.campusmarket.trade.dto;

import java.util.List;

/**
 * 중고거래 게시물 응답 DTO
 *
 * @param id          Firestore 문서 ID
 * @param title       게시물 제목
 * @param description 상품 설명
 * @param price       가격
 * @param location    거래 희망 장소
 * @param region      지역 (서울 / 경기 / 인천 등) — 등록자 가입 시 지역 자동 적용
 * @param category    카테고리 (전자기기 / 의류 / 도서 / 생활용품 / 식품 / 스포츠 / 기타)
 * @param imageUrls   이미지 URL 목록
 * @param status      거래 상태 (SELLING: 판매중 / RESERVED: 예약중 / SOLD: 판매완료)
 * @param userId      등록자 uid
 * @param viewCount   조회수
 * @param likeCount   좋아요 수
 * @param createdAt   등록 시각 (Unix 밀리초)
 */
public record TradeResponse(
    String id,
    String title,
    String description,
    Long price,
    String location,
    String region,
    String category,
    List<String> imageUrls,
    String status,
    String userId,
    Long viewCount,
    Long likeCount,
    Long createdAt
) {}
