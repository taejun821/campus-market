package com.example.campusmarket.lostitem.service;

import com.example.campusmarket.lostitem.dto.LostItemRequest;
import com.example.campusmarket.lostitem.dto.LostItemResponse;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LostItemService {

    private static final String COLLECTION = "lost_items";

    private final Firestore firestore;

    public LostItemResponse create(LostItemRequest request, String uid) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("title", request.title());
        data.put("description", request.description());
        data.put("location", request.location());
        data.put("lostDate", request.lostDate());
        data.put("imageUrls", request.imageUrls() != null ? request.imageUrls() : List.of());
        data.put("userid", uid);
        data.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference ref = firestore.collection(COLLECTION).document();
        ref.set(data).get();

        return new LostItemResponse(
            ref.getId(),
            request.title(),
            request.description(),
            request.location(),
            request.lostDate(),
            request.imageUrls(),
            uid,
            null
        );
    }

    public List<LostItemResponse> findAll() throws Exception {
        return firestore.collection(COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().get()
            .getDocuments()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public LostItemResponse findById(String id) throws Exception {
        DocumentSnapshot doc = firestore.collection(COLLECTION).document(id).get().get();
        if (!doc.exists()) {
            throw new IllegalArgumentException("존재하지 않는 분실물입니다.");
        }
        return toResponse(doc);
    }

    public void delete(String id, String uid) throws Exception {
        DocumentSnapshot doc = firestore.collection(COLLECTION).document(id).get().get();
        if (!doc.exists()) {
            throw new IllegalArgumentException("존재하지 않는 분실물입니다.");
        }
        if (!uid.equals(doc.getString("userid"))) {
            throw new SecurityException("본인이 등록한 분실물만 삭제할 수 있습니다.");
        }
        firestore.collection(COLLECTION).document(id).delete().get();
    }

    @SuppressWarnings("unchecked")
    private LostItemResponse toResponse(DocumentSnapshot doc) {
        Timestamp createdAt = doc.getTimestamp("createdAt");
        return new LostItemResponse(
            doc.getId(),
            doc.getString("title"),
            doc.getString("description"),
            doc.getString("location"),
            doc.getString("lostDate"),
            (List<String>) doc.get("imageUrls"),
            doc.getString("userid"),
            createdAt != null ? createdAt.toDate().getTime() : null
        );
    }
}
