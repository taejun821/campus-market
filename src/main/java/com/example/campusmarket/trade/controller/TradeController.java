package com.example.campusmarket.trade.controller;

import com.example.campusmarket.common.dto.ApiResponse;
import com.example.campusmarket.lostitem.dto.LikeResponse;
import com.example.campusmarket.trade.dto.TradeRequest;
import com.example.campusmarket.trade.dto.TradeResponse;
import com.example.campusmarket.trade.dto.TradeStatusRequest;
import com.example.campusmarket.trade.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 중고거래 API 컨트롤러
 *
 * Base URL: /api/trade
 * 모든 엔드포인트 JWT 인증 필요
 *
 * POST   /api/trade              - 중고거래 게시물 등록
 * GET    /api/trade              - 중고거래 목록 조회 (최신순)
 * GET    /api/trade/{id}         - 중고거래 단건 조회 (조회수 증가)
 * DELETE /api/trade/{id}         - 중고거래 게시물 삭제 (본인만)
 * PATCH  /api/trade/{id}/status  - 거래 상태 변경 (SELLING/RESERVED/SOLD, 본인만)
 * POST   /api/trade/{id}/like    - 좋아요 토글
 */
@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    // 중고거래 게시물 등록 - 성공 시 201 Created, 초기 상태는 SELLING
    @PostMapping
    public ResponseEntity<ApiResponse<TradeResponse>> create(
        @RequestBody @Valid TradeRequest request, Authentication auth
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(tradeService.create(request, uid(auth))));
    }

    // 중고거래 목록 조회 - 등록일 최신순 정렬
    @GetMapping
    public ResponseEntity<ApiResponse<List<TradeResponse>>> findAll() throws Exception {
        return ResponseEntity.ok(ApiResponse.success(tradeService.findAll()));
    }

    // 중고거래 단건 조회 - 조회할 때마다 viewCount 1 증가
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TradeResponse>> findById(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(tradeService.findById(id)));
    }

    // 중고거래 게시물 삭제 - 등록자 본인만 가능 (ForbiddenException 발생 가능)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id, Authentication auth) throws Exception {
        tradeService.delete(id, uid(auth));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 거래 상태 변경 - 본인만 가능, 허용값: SELLING·RESERVED·SOLD
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TradeResponse>> updateStatus(
        @PathVariable String id,
        @RequestBody @Valid TradeStatusRequest request,
        Authentication auth
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(tradeService.updateStatus(id, uid(auth), request)));
    }

    // 좋아요 토글 - 현재 좋아요 상태와 총 좋아요 수 반환
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(
        @PathVariable String id, Authentication auth
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(tradeService.toggleLike(id, uid(auth))));
    }

    // JWT principal에서 uid 추출
    private String uid(Authentication auth) {
        return (String) auth.getPrincipal();
    }
}
