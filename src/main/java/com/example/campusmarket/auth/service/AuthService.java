package com.example.campusmarket.auth.service;

import com.example.campusmarket.auth.dto.LoginRequest;
import com.example.campusmarket.auth.dto.RegisterRequest;
import com.example.campusmarket.auth.dto.UserResponse;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final Firestore firestore;

    @Value("${university.email.domains}")
    private List<String> allowedDomains;

    public UserResponse register(RegisterRequest request) throws Exception {
        FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(request.idToken());
        String uid = token.getUid();
        String email = token.getEmail();

        validateUniversityEmail(email);

        DocumentReference docRef = firestore.collection("users").document(uid);
        if (docRef.get().get().exists()) {
            throw new IllegalStateException("이미 가입된 사용자입니다.");
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("name", request.name());
        userData.put("university", request.university());
        userData.put("userid", uid);
        userData.put("createdAt", FieldValue.serverTimestamp());
        docRef.set(userData).get();

        return new UserResponse(uid, email, request.name(), request.university(), null);
    }

    public UserResponse login(LoginRequest request) throws Exception {
        FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(request.idToken());
        String uid = token.getUid();

        DocumentSnapshot doc = firestore.collection("users").document(uid).get().get();
        if (!doc.exists()) {
            throw new IllegalStateException("가입되지 않은 사용자입니다. 먼저 회원가입을 해주세요.");
        }

        Timestamp createdAt = doc.getTimestamp("createdAt");
        return new UserResponse(
            uid,
            doc.getString("email"),
            doc.getString("name"),
            doc.getString("university"),
            createdAt != null ? createdAt.toDate().getTime() : null
        );
    }

    private void validateUniversityEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("이메일 정보가 없습니다.");
        }
        String domain = email.substring(email.indexOf('@') + 1);
        boolean valid = allowedDomains.stream()
            .anyMatch(allowed -> domain.equals(allowed) || domain.endsWith("." + allowed));
        if (!valid) {
            throw new IllegalArgumentException("학교 이메일 주소만 가입 가능합니다. (허용 도메인: " + String.join(", ", allowedDomains) + ")");
        }
    }
}
