package com.example.campusmarket.chat.dto;

/**
 * 채팅 메시지 응답 DTO
 *
 * @param id        메시지 Firestore 문서 ID
 * @param roomId    메시지가 속한 채팅방 ID
 * @param senderId  발신자 uid
 * @param content   메시지 내용
 * @param createdAt 전송 시각 (Unix 밀리초, 전송 직후 null일 수 있음)
 */
public record MessageResponse(
    String id,
    String roomId,
    String senderId,
    String content,
    Long createdAt
) {}
