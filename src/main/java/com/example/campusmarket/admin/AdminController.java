package com.example.campusmarket.admin;

import com.example.campusmarket.common.dto.ApiResponse;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리용 API - 일회성 마이그레이션 작업
 *
 * POST /api/admin/migrate/user-name
 *   trade_items, lost_items 중 userName 없는 문서에 users 컬렉션에서 이름 조회 후 채워 넣기
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final Firestore firestore;

    @PostMapping("/migrate/user-name")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> migrateUserName() throws Exception {
        int tradeUpdated = backfill("trade_items");
        int lostUpdated = backfill("lost_items");

        Map<String, Integer> result = new HashMap<>();
        result.put("trade_items", tradeUpdated);
        result.put("lost_items", lostUpdated);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private int backfill(String collection) throws Exception {
        List<QueryDocumentSnapshot> docs = firestore.collection(collection).get().get().getDocuments();
        int count = 0;
        for (QueryDocumentSnapshot doc : docs) {
            if (doc.getString("userName") != null) continue;

            String userId = doc.getString("userId");
            if (userId == null) continue;

            DocumentSnapshot userDoc = firestore.collection("users").document(userId).get().get();
            if (!userDoc.exists()) continue;

            String userName = userDoc.getString("name");
            if (userName == null) continue;

            firestore.collection(collection).document(doc.getId())
                .update("userName", userName).get();
            count++;
        }
        return count;
    }
}
