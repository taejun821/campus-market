package com.example.campusmarket.lostitem.service;

import com.example.campusmarket.common.exception.ForbiddenException;
import com.example.campusmarket.common.exception.NotFoundException;
import com.example.campusmarket.lostitem.dto.LikeResponse;
import com.example.campusmarket.lostitem.dto.LostItemRequest;
import com.example.campusmarket.lostitem.dto.LostItemResponse;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 분실물 서비스
 * Firestore 컬렉션: "lost_items"
 *
 * 주요 기능:
 * - 분실물 등록 / 목록 조회 / 단건 조회 / 삭제
 * - 단건 조회 시 조회수(viewCount) 자동 증가
 * - 좋아요 토글 (Firestore 트랜잭션으로 동시성 처리)
 */
@Service
@RequiredArgsConstructor
public class LostItemService {

    private static final String COLLECTION = "lost_items";

    private final Firestore firestore;

    // 분실물 등록 - viewCount, likeCount 초기값 0으로 생성
    public LostItemResponse create(LostItemRequest request, String uid) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("title", request.title());
        data.put("description", request.description());
        data.put("location", request.location());
        data.put("lostDate", request.lostDate());
        data.put("imageUrls", request.imageUrls() != null ? request.imageUrls() : List.of());
        data.put("userid", uid);
        data.put("viewCount", 0L);
        data.put("likeCount", 0L);
        data.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference ref = firestore.collection(COLLECTION).document();
        ref.set(data).get();

        return new LostItemResponse(
            ref.getId(), request.title(), request.description(),
            request.location(), request.lostDate(), request.imageUrls(),
            uid, null, 0L, 0L
        );
    }

    // 목록 조회 - 최신순 정렬
    public List<LostItemResponse> findAll() throws Exception {
        return firestore.collection(COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().get()
            .getDocuments()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // 단건 조회 - 조회할 때마다 viewCount 1 증가 (FieldValue.increment: 원자적 연산)
    public LostItemResponse findById(String id) throws Exception {
        DocumentReference ref = firestore.collection(COLLECTION).document(id);
        DocumentSnapshot doc = ref.get().get();
        if (!doc.exists()) throw new NotFoundException("존재하지 않는 분실물입니다.");
        ref.update("viewCount", FieldValue.increment(1)).get();
        return toResponse(doc);
    }

    // 삭제 - 본인(userid 일치)만 가능
    public void delete(String id, String uid) throws Exception {
        DocumentSnapshot doc = firestore.collection(COLLECTION).document(id).get().get();
        if (!doc.exists()) throw new NotFoundException("존재하지 않는 분실물입니다.");
        if (!uid.equals(doc.getString("userid"))) throw new ForbiddenException("본인이 등록한 분실물만 삭제할 수 있습니다.");
        firestore.collection(COLLECTION).document(id).delete().get();
    }

    /**
     * 좋아요 토글
     * - "likes" 컬렉션에 "lost_{itemId}_{uid}" 문서로 좋아요 여부 저장
     * - 이미 좋아요 → 취소 / 없으면 → 추가
     * - Firestore 트랜잭션으로 동시 요청 시에도 likeCount 정합성 보장
     */
    public LikeResponse toggleLike(String id, String uid) throws Exception {
        DocumentReference itemRef = firestore.collection(COLLECTION).document(id);
        if (!itemRef.get().get().exists()) throw new NotFoundException("존재하지 않는 분실물입니다.");

        // likes 컬렉션 문서 ID: "lost_{itemId}_{uid}" (중복 좋아요 방지)
        DocumentReference likeRef = firestore.collection("likes").document("lost_" + id + "_" + uid);
        boolean[] liked = {false};
        long[] likeCount = {0};

        firestore.runTransaction(transaction -> {
            DocumentSnapshot likeDoc = transaction.get(likeRef).get();
            DocumentSnapshot itemDoc = transaction.get(itemRef).get();
            Long current = itemDoc.getLong("likeCount");
            if (current == null) current = 0L;

            if (likeDoc.exists()) {
                // 이미 좋아요 상태 → 취소
                transaction.delete(likeRef);
                transaction.update(itemRef, "likeCount", FieldValue.increment(-1));
                liked[0] = false;
                likeCount[0] = Math.max(0, current - 1);
            } else {
                // 좋아요 추가
                transaction.set(likeRef, Map.of(
                    "targetId", id, "type", "lost",
                    "userId", uid, "createdAt", FieldValue.serverTimestamp()
                ));
                transaction.update(itemRef, "likeCount", FieldValue.increment(1));
                liked[0] = true;
                likeCount[0] = current + 1;
            }
            return null;
        }).get();

        return new LikeResponse(liked[0], likeCount[0]);
    }

    // Firestore 문서 → LostItemResponse 변환 (null 안전 처리 포함)
    @SuppressWarnings("unchecked")
    private LostItemResponse toResponse(DocumentSnapshot doc) {
        Timestamp createdAt = doc.getTimestamp("createdAt");
        Long viewCount = doc.getLong("viewCount");
        Long likeCount = doc.getLong("likeCount");
        return new LostItemResponse(
            doc.getId(),
            doc.getString("title"),
            doc.getString("description"),
            doc.getString("location"),
            doc.getString("lostDate"),
            (List<String>) doc.get("imageUrls"),
            doc.getString("userid"),
            createdAt != null ? createdAt.toDate().getTime() : null,
            viewCount != null ? viewCount : 0L,
            likeCount != null ? likeCount : 0L
        );
    }
}
