package com.example.campusmarket.trade.service;

import com.example.campusmarket.common.dto.PageResponse;
import com.example.campusmarket.common.exception.BadRequestException;
import com.example.campusmarket.common.exception.ForbiddenException;
import com.example.campusmarket.common.exception.NotFoundException;
import com.example.campusmarket.lostitem.dto.LikeResponse;
import com.example.campusmarket.trade.dto.TradeRequest;
import com.example.campusmarket.trade.dto.TradeResponse;
import com.example.campusmarket.trade.dto.TradeStatusRequest;
import com.example.campusmarket.trade.dto.TradeUpdateRequest;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 중고거래 서비스
 * Firestore 컬렉션: "trade_items"
 *
 * 주요 기능:
 * - 중고거래 게시물 CRUD
 * - 커서 기반 페이지네이션 (createdAt 기준)
 * - 카테고리 필터링
 * - 제목 키워드 검색 (지역 내 in-memory 필터)
 * - 거래 상태 변경 (SELLING → RESERVED → SOLD)
 * - 좋아요 토글 / 좋아요한 게시물 목록
 * - 단건 조회 시 viewCount 자동 증가
 */
@Service
@RequiredArgsConstructor
public class TradeService {

    private static final String COLLECTION = "trade_items";
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final Firestore firestore;

    // 중고거래 게시물 등록 - 등록자의 지역을 자동으로 읽어 저장, 초기 상태 SELLING
    public TradeResponse create(TradeRequest request, String uid) throws Exception {
        String region = getUserRegion(uid);

        Map<String, Object> data = new HashMap<>();
        data.put("title", request.title());
        data.put("description", request.description());
        data.put("price", request.price());
        data.put("location", request.location());
        data.put("region", region);
        data.put("category", request.category());
        data.put("imageUrls", request.imageUrls() != null ? request.imageUrls() : List.of());
        data.put("status", "SELLING");
        data.put("userId", uid);
        data.put("viewCount", 0L);
        data.put("likeCount", 0L);
        data.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference ref = firestore.collection(COLLECTION).document();
        ref.set(data).get();

        return new TradeResponse(
            ref.getId(), request.title(), request.description(),
            request.price(), request.location(), region, request.category(),
            request.imageUrls(), "SELLING", uid, 0L, 0L, null
        );
    }

    /**
     * 목록 조회 - 커서 기반 페이지네이션
     * - 로그인 유저와 동일 지역 게시물만 반환
     * - category 파라미터로 카테고리 필터링 가능
     * - cursor: 마지막 항목의 createdAt(Unix ms), null이면 첫 페이지
     *
     * Firestore 복합 인덱스 필요:
     * - region ASC + createdAt DESC
     * - region ASC + category ASC + createdAt DESC (카테고리 필터 시)
     */
    public PageResponse<TradeResponse> findAll(String uid, String category, Long cursor, int size) throws Exception {
        String region = getUserRegion(uid);

        Query query = firestore.collection(COLLECTION)
            .whereEqualTo("region", region);

        if (category != null && !category.isBlank()) {
            query = query.whereEqualTo("category", category);
        }

        query = query.orderBy("createdAt", Query.Direction.DESCENDING).limit(size + 1);

        if (cursor != null) {
            Timestamp ts = Timestamp.ofTimeSecondsAndNanos(cursor / 1000, (int)((cursor % 1000) * 1_000_000));
            query = query.startAfter(ts);
        }

        List<TradeResponse> all = query.get().get()
            .getDocuments()
            .stream()
            .map(this::toResponse)
            .toList();

        boolean hasNext = all.size() > size;
        List<TradeResponse> items = hasNext ? all.subList(0, size) : all;
        Long nextCursor = hasNext ? items.get(items.size() - 1).createdAt() : null;

        return new PageResponse<>(items, nextCursor, hasNext);
    }

    /**
     * 제목 키워드 검색
     * - 로그인 유저의 지역 내에서 title에 keyword가 포함된 게시물 반환
     * - category로 추가 필터링 가능
     * - 데이터 규모가 작은 캠퍼스 특성상 in-memory 필터링 사용
     */
    public List<TradeResponse> search(String uid, String keyword, String category) throws Exception {
        String region = getUserRegion(uid);

        List<TradeResponse> results = firestore.collection(COLLECTION)
            .whereEqualTo("region", region)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().get()
            .getDocuments()
            .stream()
            .map(this::toResponse)
            .filter(r -> r.title() != null && r.title().toLowerCase().contains(keyword.toLowerCase()))
            .toList();

        if (category != null && !category.isBlank()) {
            results = results.stream()
                .filter(r -> category.equals(r.category()))
                .toList();
        }

        return results;
    }

    // 내가 등록한 목록 - 최신순
    public List<TradeResponse> findMyItems(String uid) throws Exception {
        return firestore.collection(COLLECTION)
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().get()
            .getDocuments()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * 내가 좋아요한 중고거래 목록
     * - likes 컬렉션에서 type="trade", userId=uid 인 문서 조회 후 해당 게시물 반환
     *
     * Firestore 복합 인덱스 필요: likes 컬렉션 — type ASC + userId ASC
     */
    public List<TradeResponse> findLikedItems(String uid) throws Exception {
        List<QueryDocumentSnapshot> likeDocs = firestore.collection("likes")
            .whereEqualTo("type", "trade")
            .whereEqualTo("userId", uid)
            .get().get()
            .getDocuments();

        List<TradeResponse> result = new ArrayList<>();
        for (QueryDocumentSnapshot likeDoc : likeDocs) {
            String targetId = likeDoc.getString("targetId");
            if (targetId == null) continue;
            DocumentSnapshot doc = firestore.collection(COLLECTION).document(targetId).get().get();
            if (doc.exists()) result.add(toResponse(doc));
        }
        return result;
    }

    // 단건 조회 - 조회할 때마다 viewCount 1 증가 (FieldValue.increment: 원자적 연산)
    public TradeResponse findById(String id) throws Exception {
        DocumentReference ref = firestore.collection(COLLECTION).document(id);
        DocumentSnapshot doc = ref.get().get();
        if (!doc.exists()) throw new NotFoundException("존재하지 않는 중고거래 게시물입니다.");
        ref.update("viewCount", FieldValue.increment(1)).get();
        return toResponse(ref.get().get());
    }

    // 수정 - 등록자 본인만 가능, null이 아닌 필드만 업데이트
    public TradeResponse update(String id, String uid, TradeUpdateRequest request) throws Exception {
        if (request.title() == null && request.description() == null
                && request.price() == null && request.location() == null
                && request.category() == null && request.imageUrls() == null) {
            throw new BadRequestException("수정할 내용이 없습니다.");
        }

        DocumentReference ref = firestore.collection(COLLECTION).document(id);
        DocumentSnapshot doc = ref.get().get();
        if (!doc.exists()) throw new NotFoundException("존재하지 않는 중고거래 게시물입니다.");
        if (!uid.equals(doc.getString("userId"))) throw new ForbiddenException("본인이 등록한 게시물만 수정할 수 있습니다.");

        Map<String, Object> updates = new HashMap<>();
        if (request.title() != null)       updates.put("title", request.title());
        if (request.description() != null) updates.put("description", request.description());
        if (request.price() != null)       updates.put("price", request.price());
        if (request.location() != null)    updates.put("location", request.location());
        if (request.category() != null)    updates.put("category", request.category());
        if (request.imageUrls() != null)   updates.put("imageUrls", request.imageUrls());

        ref.update(updates).get();
        return toResponse(ref.get().get());
    }

    // 삭제 - 등록자 본인(userId 일치)만 가능
    public void delete(String id, String uid) throws Exception {
        DocumentSnapshot doc = firestore.collection(COLLECTION).document(id).get().get();
        if (!doc.exists()) throw new NotFoundException("존재하지 않는 중고거래 게시물입니다.");
        if (!uid.equals(doc.getString("userId"))) throw new ForbiddenException("본인이 등록한 게시물만 삭제할 수 있습니다.");
        firestore.collection(COLLECTION).document(id).delete().get();
    }

    // 거래 상태 변경 - 등록자 본인만 가능, 변경 후 최신 문서를 읽어 반환
    public TradeResponse updateStatus(String id, String uid, TradeStatusRequest request) throws Exception {
        DocumentReference ref = firestore.collection(COLLECTION).document(id);
        DocumentSnapshot doc = ref.get().get();
        if (!doc.exists()) throw new NotFoundException("존재하지 않는 중고거래 게시물입니다.");
        if (!uid.equals(doc.getString("userId"))) throw new ForbiddenException("본인이 등록한 게시물만 수정할 수 있습니다.");
        ref.update("status", request.status()).get();
        return toResponse(ref.get().get());
    }

    // 좋아요 토글 - likes 컬렉션 "trade_{id}_{uid}" 문서로 좋아요 여부 저장, 트랜잭션으로 동시성 처리
    public LikeResponse toggleLike(String id, String uid) throws Exception {
        DocumentReference itemRef = firestore.collection(COLLECTION).document(id);
        if (!itemRef.get().get().exists()) throw new NotFoundException("존재하지 않는 중고거래 게시물입니다.");

        DocumentReference likeRef = firestore.collection("likes").document("trade_" + id + "_" + uid);
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
                    "targetId", id, "type", "trade",
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

    // Firestore 문서 → TradeResponse 변환 (null 안전 처리 포함)
    @SuppressWarnings("unchecked")
    private TradeResponse toResponse(DocumentSnapshot doc) {
        Timestamp createdAt = doc.getTimestamp("createdAt");
        Long viewCount = doc.getLong("viewCount");
        Long likeCount = doc.getLong("likeCount");
        return new TradeResponse(
            doc.getId(),
            doc.getString("title"),
            doc.getString("description"),
            doc.getLong("price"),
            doc.getString("location"),
            doc.getString("region"),
            doc.getString("category"),
            (List<String>) doc.get("imageUrls"),
            doc.getString("status"),
            doc.getString("userId"),
            viewCount != null ? viewCount : 0L,
            likeCount != null ? likeCount : 0L,
            createdAt != null ? createdAt.toDate().getTime() : null
        );
    }

    // users 컬렉션에서 uid에 해당하는 사용자의 지역 조회
    private String getUserRegion(String uid) throws Exception {
        DocumentSnapshot userDoc = firestore.collection("users").document(uid).get().get();
        if (!userDoc.exists()) throw new NotFoundException("존재하지 않는 사용자입니다.");
        String region = userDoc.getString("region");
        if (region == null) throw new BadRequestException("지역 정보가 없습니다. 프로필을 업데이트해 주세요.");
        return region;
    }
}
