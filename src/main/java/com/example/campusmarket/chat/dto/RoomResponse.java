package com.example.campusmarket.chat.dto;

import java.util.List;

/**
 * 채팅방 응답 DTO
 *
 * @param id            채팅방 ID ({uid1}_{uid2}_{itemId} 형식, uid는 사전순 정렬)
 * @param participants  채팅 참여자 uid 목록
 * @param itemId        채팅 대상 아이템 ID
 * @param itemType      아이템 종류 ("lost" 또는 "trade")
 * @param lastMessage   마지막 메시지 내용 (채팅 목록 미리보기용, 없으면 null)
 * @param lastMessageAt 마지막 메시지 전송 시각 (Unix 밀리초, 없으면 null)
 * @param createdAt     채팅방 생성 시각 (Unix 밀리초)
 * @param unreadCount   요청한 유저의 읽지 않은 메시지 수
 */
public record RoomResponse(
    String id,
    List<String> participants,
    String itemId,
    String itemType,
    String lastMessage,
    Long lastMessageAt,
    Long createdAt,
    Long unreadCount
) {}
