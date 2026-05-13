package com.example.campusmarket.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 채팅방 생성/조회 요청 DTO
 *
 * @param targetUserId 채팅 상대방 uid (필수)
 * @param itemId       채팅 대상 아이템 ID (필수, 분실물 또는 중고거래 게시물 ID)
 * @param itemType     아이템 종류 (필수, "lost" 또는 "trade")
 */
public record RoomRequest(
    @NotBlank(message = "상대방 UID는 필수입니다.") String targetUserId,
    @NotBlank(message = "아이템 ID는 필수입니다.") String itemId,
    @NotBlank
    @Pattern(regexp = "lost|trade", message = "itemType은 lost 또는 trade여야 합니다.")
    String itemType
) {}
