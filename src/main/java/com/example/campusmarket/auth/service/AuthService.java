package com.example.campusmarket.auth.service;

import com.example.campusmarket.auth.dto.LoginRequest;
import com.example.campusmarket.auth.dto.RegisterRequest;
import com.example.campusmarket.auth.dto.UserResponse;
import com.example.campusmarket.common.exception.BadRequestException;
import com.example.campusmarket.common.exception.DuplicateException;
import com.example.campusmarket.common.exception.NotFoundException;
import com.example.campusmarket.common.util.JwtUtil;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 회원가입 / 로그인 처리 서비스
 *
 * 사용자 데이터 저장소: Firestore "users" 컬렉션
 * 인증 방식: 이메일 + 비밀번호 (BCrypt 해시) → JWT 발급
 * 이메일 제한: application.properties의 university.email.domains에 등록된 도메인만 허용
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final Firestore firestore;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // 허용된 학교 이메일 도메인 목록 (예: ac.kr)
    @Value("${university.email.domains}")
    private List<String> allowedDomains;

    /**
     * 회원가입
     * 1. 학교 이메일 도메인 검증
     * 2. 이메일 중복 확인
     * 3. 비밀번호 BCrypt 해시 후 Firestore 저장
     * 4. JWT 발급 후 반환
     */
    public UserResponse register(RegisterRequest request) throws Exception {
        validateUniversityEmail(request.email());

        // 이메일 중복 확인
        QuerySnapshot existing = firestore.collection("users")
            .whereEqualTo("email", request.email())
            .get().get();
        if (!existing.isEmpty()) {
            throw new DuplicateException("이미 가입된 이메일입니다.");
        }

        String uid = UUID.randomUUID().toString();

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", request.email());
        userData.put("name", request.name());
        userData.put("university", request.university());
        userData.put("password", passwordEncoder.encode(request.password())); // 평문 저장 금지
        userData.put("userid", uid);
        userData.put("createdAt", FieldValue.serverTimestamp());
        firestore.collection("users").document(uid).set(userData).get();

        return new UserResponse(uid, request.email(), request.name(), request.university(), null, jwtUtil.generateToken(uid));
    }

    /**
     * 로그인
     * 1. 이메일로 사용자 조회
     * 2. BCrypt로 비밀번호 일치 확인
     * 3. JWT 발급 후 반환
     */
    public UserResponse login(LoginRequest request) throws Exception {
        // 이메일로 사용자 검색 (단일 결과만 필요하므로 limit(1))
        QuerySnapshot snapshot = firestore.collection("users")
            .whereEqualTo("email", request.email())
            .limit(1)
            .get().get();

        if (snapshot.isEmpty()) {
            throw new NotFoundException("가입되지 않은 이메일입니다.");
        }

        DocumentSnapshot doc = snapshot.getDocuments().get(0);
        String storedHash = doc.getString("password");

        // BCrypt matches(): 평문 비밀번호와 저장된 해시 비교
        if (storedHash == null || !passwordEncoder.matches(request.password(), storedHash)) {
            throw new BadRequestException("비밀번호가 올바르지 않습니다.");
        }

        String uid = doc.getString("userid");
        Timestamp createdAt = doc.getTimestamp("createdAt");

        return new UserResponse(
            uid,
            doc.getString("email"),
            doc.getString("name"),
            doc.getString("university"),
            createdAt != null ? createdAt.toDate().getTime() : null,
            jwtUtil.generateToken(uid)
        );
    }

    // 이메일 도메인이 허용 목록에 있는지 확인 (예: pcu.ac.kr → ac.kr 포함)
    private void validateUniversityEmail(String email) {
        String domain = email.substring(email.indexOf('@') + 1);
        boolean valid = allowedDomains.stream()
            .anyMatch(allowed -> domain.equals(allowed) || domain.endsWith("." + allowed));
        if (!valid) {
            throw new BadRequestException("학교 이메일 주소만 가입 가능합니다. (허용 도메인: " + String.join(", ", allowedDomains) + ")");
        }
    }
}
