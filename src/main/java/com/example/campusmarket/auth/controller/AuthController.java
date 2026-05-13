package com.example.campusmarket.auth.controller;

import com.example.campusmarket.auth.dto.LoginRequest;
import com.example.campusmarket.auth.dto.RegisterRequest;
import com.example.campusmarket.auth.dto.UserResponse;
import com.example.campusmarket.auth.service.AuthService;
import com.example.campusmarket.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러
 *
 * Base URL: /api/auth
 * 인증 토큰 불필요 (SecurityConfig에서 /api/auth/** 전체 허용)
 *
 * POST /api/auth/register - 회원가입
 * POST /api/auth/login    - 로그인
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
}
