package com.example.campusmarket.common.filter;

import com.example.campusmarket.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터 - 모든 요청에 대해 1회 실행
 *
 * 처리 흐름:
 * 1. Authorization 헤더에서 "Bearer {token}" 추출
 * 2. 토큰 유효성 검증 (서명 + 만료)
 * 3. 유효하면 uid를 SecurityContext에 저장 → 이후 컨트롤러에서 Authentication으로 꺼내 사용
 * 4. 토큰 없거나 유효하지 않으면 그냥 통과 (인증 필요 API는 SecurityConfig에서 차단)
 *
 * Flutter에서 호출 예시:
 * Authorization: Bearer eyJhbGci...
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // "Bearer " 이후 토큰 값만 추출
            if (jwtUtil.validateToken(token)) {
                String uid = jwtUtil.extractUid(token);
                // uid를 principal로 저장 → 컨트롤러에서 (String) auth.getPrincipal()로 꺼냄
                if (uid != null) {
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(uid, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
