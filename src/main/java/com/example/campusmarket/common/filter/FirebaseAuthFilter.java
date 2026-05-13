package com.example.campusmarket.common.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Firebase ID 토큰 검증 필터 (현재 미사용 - JWT 방식(JwtAuthFilter)으로 대체됨)
 *
 * Firebase Authentication에서 발급한 ID 토큰을 Authorization 헤더에서 추출하여 검증한다.
 * Firebase 인증으로 전환 시 SecurityConfig에 이 필터를 등록하면 된다.
 */
public class FirebaseAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        // Authorization 헤더가 없거나 Bearer 방식이 아니면 인증 없이 다음 필터로 통과
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = header.substring(7); // "Bearer " 접두어를 제거한 실제 토큰
        try {
            // Firebase SDK로 ID 토큰 검증 (서명·만료·프로젝트 ID 일치 확인)
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(decoded, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (FirebaseAuthException e) {
            // 토큰 검증 실패 시 SecurityContext 초기화 → 미인증 상태로 계속 처리
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
