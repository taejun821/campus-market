package com.example.campusmarket.chat.service;

import com.example.campusmarket.chat.dto.MessageRequest;
import com.example.campusmarket.chat.dto.MessageResponse;
import com.example.campusmarket.chat.dto.RoomRequest;
import com.example.campusmarket.chat.dto.RoomResponse;
import com.example.campusmarket.common.dto.PageResponse;
import com.example.campusmarket.common.exception.ForbiddenException;
import com.example.campusmarket.common.exception.NotFoundException;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import java.util.ArrayList;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 채팅 서비스
 * Firestore 컬렉션:
 * - "chat_rooms"   : 채팅방 정보 (참여자, 연결된 아이템, 마지막 메시지)
 * - "chat_messages": 개별 메시지
 *
 * 채팅방 ID 규칙: {uid1}_{uid2}_{itemId}
 * - uid를 사전순 정렬 후 조합 → 두 사용자가 같은 방을 항상 동일한 ID로 참조
 * - 같은 아이템에 대해 두 사용자 간 채팅방이 중복 생성되지 않음
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String ROOMS = "chat_rooms";
    private static final String MESSAGES = "chat_messages";

    private final Firestore firestore;

    /**
     * 채팅방 조회 또는 생성
     * - 두 사용자 + 아이템 조합의 방이 이미 있으면 기존 방 반환
     * - 없으면 새 방 생성
     */
    public RoomResponse getOrCreateRoom(RoomRequest request, String uid) throws Exception {
        // uid를 사전순 정렬해 항상 동일한 roomId 생성 (A↔B == B↔A)
        String a = uid.compareTo(request.targetUserId()) < 0 ? uid : request.targetUserId();
        String b = uid.compareTo(request.targetUserId()) < 0 ? request.targetUserId() : uid;
        String roomId = a + "_" + b + "_" + request.itemId();

        DocumentReference ref = firestore.collection(ROOMS).document(roomId);
        DocumentSnapshot doc = ref.get().get();

        if (doc.exists()) {
            return toRoomResponse(doc, uid);
        }

        // 새 채팅방 생성 - unreadCount 맵으로 두 참여자의 읽지 않은 메시지 수 관리
        Map<String, Object> data = new HashMap<>();
        data.put("participants", List.of(uid, request.targetUserId()));
        data.put("itemId", request.itemId());
        data.put("itemType", request.itemType());
        data.put("lastMessage", null);
        data.put("lastMessageAt", null);
        data.put("unreadCount", Map.of(uid, 0L, request.targetUserId(), 0L));
        data.put("createdAt", FieldValue.serverTimestamp());
        ref.set(data).get();

        return toRoomResponse(ref.get().get(), uid);
    }

    // 내가 참여한 채팅방 목록 (최신순)
    public List<RoomResponse> getMyRooms(String uid) throws Exception {
        return firestore.collection(ROOMS)
            .whereArrayContains("participants", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().get()
            .getDocuments()
            .stream()
            .map(doc -> toRoomResponse(doc, uid))
            .toList();
    }

    // 읽음 처리 - 내 unreadCount를 0으로 리셋
    public void markAsRead(String roomId, String uid) throws Exception {
        DocumentSnapshot room = firestore.collection(ROOMS).document(roomId).get().get();
        if (!room.exists()) throw new NotFoundException("존재하지 않는 채팅방입니다.");

        @SuppressWarnings("unchecked")
        List<String> participants = (List<String>) room.get("participants");
        if (participants == null || !participants.contains(uid)) {
            throw new ForbiddenException("채팅방 참여자만 읽음 처리할 수 있습니다.");
        }

        firestore.collection(ROOMS).document(roomId)
            .update("unreadCount." + uid, 0L).get();
    }

    /**
     * 메시지 조회 - 커서 기반 페이지네이션
     * - 참여자만 가능
     * - 기본: 최신 메시지부터 size개 반환 (cursor 없으면 첫 로드)
     * - cursor 있으면: 해당 시점보다 오래된 메시지 반환 (위로 스크롤 시 이전 메시지 로드)
     * - 반환 순서: 오래된 순 (화면 표시용)
     */
    public PageResponse<MessageResponse> getMessages(String roomId, String uid, Long cursor, int size) throws Exception {
        DocumentSnapshot room = firestore.collection(ROOMS).document(roomId).get().get();
        if (!room.exists()) throw new NotFoundException("존재하지 않는 채팅방입니다.");

        @SuppressWarnings("unchecked")
        List<String> participants = (List<String>) room.get("participants");
        if (participants == null || !participants.contains(uid)) {
            throw new ForbiddenException("채팅방 참여자만 메시지를 조회할 수 있습니다.");
        }

        // 최신순으로 내려받아 커서 이전 메시지 로드 (위로 스크롤)
        Query query = firestore.collection(MESSAGES)
            .whereEqualTo("roomId", roomId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(size + 1);

        if (cursor != null) {
            Timestamp ts = Timestamp.ofTimeSecondsAndNanos(cursor / 1000, (int)((cursor % 1000) * 1_000_000));
            query = query.startAfter(ts);
        }

        List<MessageResponse> all = query.get().get().getDocuments().stream()
            .map(doc -> {
                Timestamp createdAt = doc.getTimestamp("createdAt");
                return new MessageResponse(
                    doc.getId(), doc.getString("roomId"), doc.getString("senderId"),
                    doc.getString("content"),
                    createdAt != null ? createdAt.toDate().getTime() : null
                );
            })
            .toList();

        boolean hasNext = all.size() > size;
        List<MessageResponse> items = hasNext ? all.subList(0, size) : all;
        // nextCursor = 가장 오래된 메시지의 createdAt (다음 "더 보기" 요청 시 사용)
        Long nextCursor = hasNext ? items.get(items.size() - 1).createdAt() : null;

        // 화면 표시용으로 오래된 순 정렬
        List<MessageResponse> chronological = new ArrayList<>(items);
        Collections.reverse(chronological);

        return new PageResponse<>(chronological, nextCursor, hasNext);
    }

    /**
     * 메시지 전송
     * - 참여자 여부 확인 후 메시지 저장
     * - 채팅방의 lastMessage, lastMessageAt 동시 업데이트 (목록에서 미리보기용)
     */
    public MessageResponse sendMessage(String roomId, MessageRequest request, String uid) throws Exception {
        DocumentReference roomRef = firestore.collection(ROOMS).document(roomId);
        DocumentSnapshot room = roomRef.get().get();
        if (!room.exists()) throw new NotFoundException("존재하지 않는 채팅방입니다.");

        @SuppressWarnings("unchecked")
        List<String> participants = (List<String>) room.get("participants");
        if (participants == null || !participants.contains(uid)) {
            throw new ForbiddenException("채팅방 참여자만 메시지를 보낼 수 있습니다.");
        }

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("roomId", roomId);
        msgData.put("senderId", uid);
        msgData.put("content", request.content());
        msgData.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference msgRef = firestore.collection(MESSAGES).document();
        msgRef.set(msgData).get();

        // 채팅방 lastMessage 갱신 + 상대방 unreadCount +1
        Map<String, Object> roomUpdates = new HashMap<>();
        roomUpdates.put("lastMessage", request.content());
        roomUpdates.put("lastMessageAt", FieldValue.serverTimestamp());
        for (String other : participants.stream().filter(p -> !p.equals(uid)).toList()) {
            roomUpdates.put("unreadCount." + other, FieldValue.increment(1));
        }
        roomRef.update(roomUpdates).get();

        return new MessageResponse(msgRef.getId(), roomId, uid, request.content(), null);
    }

    @SuppressWarnings("unchecked")
    private RoomResponse toRoomResponse(DocumentSnapshot doc, String uid) {
        Timestamp createdAt = doc.getTimestamp("createdAt");
        Timestamp lastMessageAt = doc.getTimestamp("lastMessageAt");
        Map<String, Object> unreadCountMap = (Map<String, Object>) doc.get("unreadCount");
        Long unreadCount = 0L;
        if (unreadCountMap != null && unreadCountMap.get(uid) instanceof Number n) {
            unreadCount = n.longValue();
        }
        return new RoomResponse(
            doc.getId(),
            (List<String>) doc.get("participants"),
            doc.getString("itemId"),
            doc.getString("itemType"),
            doc.getString("lastMessage"),
            lastMessageAt != null ? lastMessageAt.toDate().getTime() : null,
            createdAt != null ? createdAt.toDate().getTime() : null,
            unreadCount
        );
    }
}
