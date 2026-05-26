package com.example.campusmarket.chat.controller;

import com.example.campusmarket.chat.dto.MessageRequest;
import com.example.campusmarket.chat.dto.MessageResponse;
import com.example.campusmarket.chat.dto.RoomRequest;
import com.example.campusmarket.chat.dto.RoomResponse;
import com.example.campusmarket.chat.service.ChatService;
import com.example.campusmarket.common.dto.ApiResponse;
import com.example.campusmarket.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 채팅 API 컨트롤러
 *
 * Base URL: /api/chat
 * 모든 엔드포인트 JWT 인증 필요
 *
 * POST /api/chat/rooms                      - 채팅방 조회 또는 생성 (이미 있으면 기존 방 반환)
 * GET  /api/chat/rooms                      - 내가 참여한 채팅방 목록 조회 (최신순)
 * GET  /api/chat/rooms/{roomId}/messages    - 채팅방 메시지 목록 조회 (최대 100개, 오래된 순)
 * POST /api/chat/rooms/{roomId}/messages    - 메시지 전송
 * POST /api/chat/rooms/{roomId}/read        - 읽음 처리 (내 unreadCount 0으로 리셋)
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 채팅방 조회 또는 생성 - 두 사용자 + 아이템 조합으로 고유 roomId 생성, 기존 방 있으면 반환
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<RoomResponse>> getOrCreateRoom(
        @RequestBody @Valid RoomRequest request, Authentication auth
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.success(chatService.getOrCreateRoom(request, uid(auth))));
    }

    // 내가 참여한 채팅방 목록 조회 - participants 배열에 내 uid가 포함된 방만 반환
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getMyRooms(Authentication auth) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(chatService.getMyRooms(uid(auth))));
    }

    // 메시지 조회 - 커서 기반 페이지네이션, ?cursor=&size=30
    // cursor 없으면 최신 메시지부터, cursor 있으면 그 이전 메시지 (위로 스크롤 시 이전 메시지 로드)
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessages(
        @PathVariable String roomId,
        Authentication auth,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "30") int size
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(chatService.getMessages(roomId, uid(auth), cursor, size)));
    }

    // 메시지 전송 - 참여자만 가능, 전송 후 채팅방 lastMessage 업데이트, 성공 시 201 Created
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
        @PathVariable String roomId,
        @RequestBody @Valid MessageRequest request,
        Authentication auth
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(chatService.sendMessage(roomId, request, uid(auth))));
    }

    // 읽음 처리 - 채팅방 열 때 호출, 내 unreadCount를 0으로 리셋
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
        @PathVariable String roomId, Authentication auth
    ) throws Exception {
        chatService.markAsRead(roomId, uid(auth));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // JWT principal에서 uid 추출
    private String uid(Authentication auth) {
        return (String) auth.getPrincipal();
    }
}
