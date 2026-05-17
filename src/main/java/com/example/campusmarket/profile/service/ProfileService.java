package com.example.campusmarket.profile.service;

import com.example.campusmarket.common.exception.BadRequestException;
import com.example.campusmarket.common.exception.NotFoundException;
import com.example.campusmarket.profile.dto.ProfileResponse;
import com.example.campusmarket.profile.dto.ProfileUpdateRequest;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 프로필 서비스
 * Firestore 컬렉션: "users"
 *
 * 주요 기능:
 * - 내 프로필 조회 (이메일 포함)
 * - 공개 프로필 조회 (이메일 미포함)
 * - 프로필 수정 (이름 / 대학교명 / 지역 변경 가능)
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final Firestore firestore;

    // 내 프로필 조회 - 이메일을 포함한 전체 정보 반환
    public ProfileResponse getMyProfile(String uid) throws Exception {
        return fetchProfile(uid);
    }

    // 공개 프로필 조회 - 타인의 프로필이므로 이메일을 null로 설정해 반환
    public ProfileResponse getPublicProfile(String uid) throws Exception {
        DocumentSnapshot doc = firestore.collection("users").document(uid).get().get();
        if (!doc.exists()) throw new NotFoundException("존재하지 않는 사용자입니다.");
        Timestamp createdAt = doc.getTimestamp("createdAt");
        // 공개 프로필은 이메일 미포함
        return new ProfileResponse(
            uid, null, doc.getString("name"), doc.getString("university"),
            doc.getString("region"), createdAt != null ? createdAt.toDate().getTime() : null
        );
    }

    // 프로필 수정 - name / university / region 중 전달된 필드만 업데이트
    public ProfileResponse updateProfile(String uid, ProfileUpdateRequest request) throws Exception {
        if (request.name() == null && request.university() == null && request.region() == null) {
            throw new BadRequestException("수정할 내용이 없습니다.");
        }

        Map<String, Object> updates = new HashMap<>();
        if (request.name() != null)       updates.put("name", request.name());
        if (request.university() != null) updates.put("university", request.university());
        if (request.region() != null)     updates.put("region", request.region());

        firestore.collection("users").document(uid).update(updates).get();
        return fetchProfile(uid);
    }

    // Firestore "users" 컬렉션에서 uid 문서를 읽어 ProfileResponse로 변환
    private ProfileResponse fetchProfile(String uid) throws Exception {
        DocumentSnapshot doc = firestore.collection("users").document(uid).get().get();
        if (!doc.exists()) throw new NotFoundException("사용자를 찾을 수 없습니다.");
        Timestamp createdAt = doc.getTimestamp("createdAt");
        return new ProfileResponse(
            uid, doc.getString("email"), doc.getString("name"),
            doc.getString("university"), doc.getString("region"),
            createdAt != null ? createdAt.toDate().getTime() : null
        );
    }
}
