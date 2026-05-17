package com.example.campusmarket.auth.controller;

import com.example.campusmarket.auth.dto.AccountDeleteRequest;
import com.example.campusmarket.auth.dto.LoginRequest;
import com.example.campusmarket.auth.dto.PasswordChangeRequest;
import com.example.campusmarket.auth.dto.RegisterRequest;
import com.example.campusmarket.auth.dto.UserResponse;
import com.example.campusmarket.auth.service.AuthService;
import com.example.campusmarket.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러
 *
 * Base URL: /api/auth
 *
 * POST   /api/auth/register  - 회원가입 (인증 불필요)
 * POST   /api/auth/login     - 로그인 (인증 불필요)
 * PUT    /api/auth/password  - 비밀번호 변경 (JWT 필요)
 * DELETE /api/auth/me        - 회원 탈퇴 (JWT 필요)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입 - 성공 시 201 Created와 사용자 정보(JWT 포함) 반환
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody @Valid RegisterRequest request) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(authService.register(request)));
    }

    // 로그인 - 성공 시 200 OK와 사용자 정보(JWT 포함) 반환
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@RequestBody @Valid LoginRequest request) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    // 비밀번호 변경 - 현재 비밀번호 확인 후 새 비밀번호로 변경
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
        @RequestBody @Valid PasswordChangeRequest request, Authentication auth
    ) throws Exception {
        authService.changePassword(uid(auth), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 회원 탈퇴 - 비밀번호 확인 후 사용자 및 게시물 데이터 삭제
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
        @RequestBody @Valid AccountDeleteRequest request, Authentication auth
    ) throws Exception {
        authService.deleteAccount(uid(auth), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // JWT principal에서 uid 추출
    private String uid(Authentication auth) {
        return (String) auth.getPrincipal();
    }
}
