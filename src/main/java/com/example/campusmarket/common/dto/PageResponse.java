package com.example.campusmarket.common.dto;

import java.util.List;

/**
 * 커서 기반 페이지네이션 응답 래퍼
 *
 * @param items      현재 페이지 항목 목록
 * @param nextCursor 다음 페이지 요청 시 cursor 파라미터로 전달할 값 (마지막 항목의 createdAt, 다음 페이지 없으면 null)
 * @param hasNext    다음 페이지 존재 여부
 */
public record PageResponse<T>(
    List<T> items,
    Long nextCursor,
    boolean hasNext
) {}
