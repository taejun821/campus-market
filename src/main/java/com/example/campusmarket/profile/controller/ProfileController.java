package com.example.campusmarket.profile.controller;

import com.example.campusmarket.common.dto.ApiResponse;
import com.example.campusmarket.profile.dto.ProfileResponse;
import com.example.campusmarket.profile.dto.ProfileUpdateRequest;
import com.example.campusmarket.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로필 API 컨트롤러
 *
 * Base URL: /api/profile
 * 모든 엔드포인트 JWT 인증 필요
 *
 * GET /api/profile        - 내 프로필 조회 (이메일 포함)
 * PUT /api/profile        - 내 프로필 수정 (이름 변경)
 * GET /api/profile/{uid}  - 타인 공개 프로필 조회 (이메일 미포함)
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // 내 프로필 조회 - JWT에서 추출한 uid로 Firestore 사용자 문서 반환
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(Authentication auth) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(profileService.getMyProfile(uid(auth))));
    }

    // 내 프로필 수정 - 현재는 이름만 변경 가능
    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
        @RequestBody @Valid ProfileUpdateRequest request, Authentication auth
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateProfile(uid(auth), request)));
    }

    // 타인 공개 프로필 조회 - 이메일은 포함하지 않음 (개인정보 보호)
    @GetMapping("/{uid}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getPublicProfile(@PathVariable String uid) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(profileService.getPublicProfile(uid)));
    }

    // JWT principal에서 uid(사용자 고유 ID) 추출
    private String uid(Authentication auth) {
        return (String) auth.getPrincipal();
    }
}
