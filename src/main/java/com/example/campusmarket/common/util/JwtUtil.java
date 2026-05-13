package com.example.campusmarket.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 생성 / 검증 유틸리티
 *
 * 토큰 구조: Header.Payload.Signature
 * - Payload에 사용자 uid(subject)와 만료 시간 포함
 * - 알고리즘: HS384 (HMAC + SHA-384)
 *
 * application.properties 설정값:
 * - jwt.secret    : Base64 인코딩된 서명 키 (최소 32바이트)
 * - jwt.expiration: 토큰 유효 시간 (밀리초, 기본 86400000 = 24시간)
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration}") long expiration
    ) {
        // Base64 디코딩 후 HMAC 서명 키 생성
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiration = expiration;
    }

    // 로그인/회원가입 성공 시 uid를 subject로 담아 토큰 발급
    public String generateToken(String uid) {
        return Jwts.builder()
            .subject(uid)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(secretKey)
            .compact();
    }

    // 토큰에서 uid 추출 (API 요청 처리 시 현재 사용자 식별에 사용)
    public String extractUid(String token) {
        return parseClaims(token).getSubject();
    }

    // 서명 검증 + 만료 여부 확인 (JwtAuthFilter에서 요청마다 호출)
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
