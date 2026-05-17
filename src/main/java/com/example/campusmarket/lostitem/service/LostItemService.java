package com.example.campusmarket.lostitem.service;

import com.example.campusmarket.common.exception.BadRequestException;
import com.example.campusmarket.common.exception.ForbiddenException;
import com.example.campusmarket.common.exception.NotFoundException;
import com.example.campusmarket.lostitem.dto.LikeResponse;
import com.example.campusmarket.lostitem.dto.LostItemRequest;
import com.example.campusmarket.lostitem.dto.LostItemResponse;
import com.example.campusmarket.lostitem.dto.LostItemStatusRequest;
import com.example.campusmarket.lostitem.dto.LostItemUpdateRequest;
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
 * - 분실물 등록 / 목록 조회 / 단건 조회 / 수정 / 삭제 / 상태 변경
 * - 단건 조회 시 조회수(viewCount) 자동 증가
 * - 좋아요 토글 (Firestore 트랜잭션으로 동시성 처리)
 */
@Service
@RequiredArgsConstructor
public class LostItemService {

    private static final String COLLECTION = "lost_items";

    private final Firestore firestore;

    // 분실물 등록 - 등록자의 지역을 자동으로 읽어 저장, 초기 상태 LOST
    public LostItemResponse create(LostItemRequest request, String uid) throws Exception {
        String region = getUserRegion(uid);

        Map<String, Object> data = new HashMap<>();
        data.put("title", request.title());
        data.put("description", request.description());
        data.put("location", request.location());
        data.put("lostDate", request.lostDate());
        data.put("region", region);
        data.put("imageUrls", request.imageUrls() != null ? request.imageUrls() : List.of());
        data.put("status", "LOST");
        data.put("userid", uid);
        data.put("viewCount", 0L);
        data.put("likeCount", 0L);
        data.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference ref = firestore.collection(COLLECTION).document();
        ref.set(data).get();

        return new LostItemResponse(
            ref.getId(), request.title(), request.description(),
            request.location(), request.lostDate(), region, request.imageUrls(),
            "LOST", uid, null, 0L, 0L
        );
    }

    // 목록 조회 - 로그인 유저와 동일 지역 게시물만, 최신순 정렬
    public List<LostItemResponse> findAll(String uid) throws Exception {
        String region = getUserRegion(uid);
        return firestore.collection(COLLECTION)
            .whereEqualTo("region", region)
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
        return toResponse(ref.get().get());
    }

    // 내가 등록한 분실물 목록 - 최신순
    public List<LostItemResponse> findMyItems(String uid) throws Exception {
        return firestore.collection(COLLECTION)
            .whereEqualTo("userid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().get()
            .getDocuments()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // 수정 - 본인만 가능, null이 아닌 필드만 업데이트
    public LostItemResponse update(String id, String uid, LostItemUpdateRequest request) throws Exception {
        if (request.title() == null && request.description() == null
                && request.location() == null && request.lostDate() == null
                && request.imageUrls() == null) {
            throw new BadRequestException("수정할 내용이 없습니다.");
        }

        DocumentReference ref = firestore.collection(COLLECTION).document(id);
        DocumentSnapshot doc = ref.get().get();
        if (!doc.exists()) throw new NotFoundException("존재하지 않는 분실물입니다.");
        if (!uid.equals(doc.getString("userid"))) throw new ForbiddenException("본인이 등록한 분실물만 수정할 수 있습니다.");

        Map<String, Object> updates = new HashMap<>();
        if (request.title() != null)       updates.put("title", request.title());
        if (request.description() != null) updates.put("description", request.description());
        if (request.location() != null)    updates.put("location", request.location());
        if (request.lostDate() != null)    updates.put("lostDate", request.lostDate());
        if (request.imageUrls() != null)   updates.put("imageUrls", request.imageUrls());

        ref.update(updates).get();
        return toResponse(ref.get().get());
    }

    // 상태 변경 - 본인만 가능 (LOST: 분실중 / FOUND: 찾았음)
    public LostItemResponse updateStatus(String id, String uid, LostItemStatusRequest request) throws Exception {
        DocumentReference ref = firestore.collection(COLLECTION).document(id);
        DocumentSnapshot doc = ref.get().get();
        if (!doc.exists()) throw new NotFoundException("존재하지 않는 분실물입니다.");
        if (!uid.equals(doc.getString("userid"))) throw new ForbiddenException("본인이 등록한 분실물만 상태를 변경할 수 있습니다.");

        ref.update("status", request.status()).get();
        return toResponse(ref.get().get());
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

        DocumentReference likeRef = firestore.collection("likes").document("lost_" + id + "_" + uid);
        boolean[] liked = {false};
        long[] likeCount = {0};

        firestore.runTransaction(transaction -> {
            DocumentSnapshot likeDoc = transaction.get(likeRef).get();
            DocumentSnapshot itemDoc = transaction.get(itemRef).get();
            Long current = itemDoc.getLong("likeCount");
            if (current == null) current = 0L;

            if (likeDoc.exists()) {
                transaction.delete(likeRef);
                transaction.update(itemRef, "likeCount", FieldValue.increment(-1));
                liked[0] = false;
                likeCount[0] = Math.max(0, current - 1);
            } else {
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
        String status = doc.getString("status");
        return new LostItemResponse(
            doc.getId(),
            doc.getString("title"),
            doc.getString("description"),
            doc.getString("location"),
            doc.getString("lostDate"),
            doc.getString("region"),
            (List<String>) doc.get("imageUrls"),
            status != null ? status : "LOST",
            doc.getString("userid"),
            createdAt != null ? createdAt.toDate().getTime() : null,
            viewCount != null ? viewCount : 0L,
            likeCount != null ? likeCount : 0L
        );
    }

    // users 컬렉션에서 uid에 해당하는 사용자의 지역 조회
    private String getUserRegion(String uid) throws Exception {
        DocumentSnapshot userDoc = firestore.collection("users").document(uid).get().get();
        if (!userDoc.exists()) throw new com.example.campusmarket.common.exception.NotFoundException("존재하지 않는 사용자입니다.");
        String region = userDoc.getString("region");
        if (region == null) throw new com.example.campusmarket.common.exception.BadRequestException("지역 정보가 없습니다. 프로필을 업데이트해 주세요.");
        return region;
    }
}
