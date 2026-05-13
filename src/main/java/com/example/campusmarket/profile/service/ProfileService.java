package com.example.campusmarket.profile.service;

import com.example.campusmarket.common.exception.NotFoundException;
import com.example.campusmarket.profile.dto.ProfileResponse;
import com.example.campusmarket.profile.dto.ProfileUpdateRequest;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 프로필 서비스
 * Firestore 컬렉션: "users"
 *
 * 주요 기능:
 * - 내 프로필 조회 (이메일 포함)
 * - 공개 프로필 조회 (이메일 미포함)
 * - 프로필 수정 (이름 변경)
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
            createdAt != null ? createdAt.toDate().getTime() : null
        );
    }

    // 이름 업데이트 후 변경된 프로필 조회해 반환
    public ProfileResponse updateProfile(String uid, ProfileUpdateRequest request) throws Exception {
        firestore.collection("users").document(uid).update("name", request.name()).get();
        return fetchProfile(uid);
    }

    // Firestore "users" 컬렉션에서 uid 문서를 읽어 ProfileResponse로 변환
    private ProfileResponse fetchProfile(String uid) throws Exception {
        DocumentSnapshot doc = firestore.collection("users").document(uid).get().get();
        if (!doc.exists()) throw new NotFoundException("사용자를 찾을 수 없습니다.");
        Timestamp createdAt = doc.getTimestamp("createdAt");
        return new ProfileResponse(
            uid, doc.getString("email"), doc.getString("name"), doc.getString("university"),
            createdAt != null ? createdAt.toDate().getTime() : null
        );
    }
}
