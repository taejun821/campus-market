package com.example.campusmarket.trade.controller;

import com.example.campusmarket.common.dto.ApiResponse;
import com.example.campusmarket.common.dto.PageResponse;
import com.example.campusmarket.lostitem.dto.LikeResponse;
import com.example.campusmarket.trade.dto.TradeRequest;
import com.example.campusmarket.trade.dto.TradeResponse;
import com.example.campusmarket.trade.dto.TradeStatusRequest;
import com.example.campusmarket.trade.dto.TradeUpdateRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 중고거래 API 컨트롤러
 *
 * Base URL: /api/trade
 * 모든 엔드포인트 JWT 인증 필요
 *
 * POST   /api/trade                  - 중고거래 게시물 등록
 * GET    /api/trade                  - 중고거래 목록 조회 (지역 필터, 페이지네이션, 카테고리 필터)
 * GET    /api/trade/search           - 제목 키워드 검색
 * GET    /api/trade/my               - 내가 등록한 목록
 * GET    /api/trade/liked            - 내가 좋아요한 목록
 * GET    /api/trade/{id}             - 단건 조회 (조회수 증가)
 * PUT    /api/trade/{id}             - 게시물 수정 (본인만)
 * DELETE /api/trade/{id}             - 게시물 삭제 (본인만)
 * PATCH  /api/trade/{id}/status      - 거래 상태 변경 (SELLING/RESERVED/SOLD, 본인만)
 * POST   /api/trade/{id}/like        - 좋아요 토글
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

    // 중고거래 목록 조회 - 동일 지역 게시물, 커서 기반 페이지네이션
    // ?category=전자기기&cursor=1716000000000&size=20
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TradeResponse>>> findAll(
        Authentication auth,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "20") int size
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(tradeService.findAll(uid(auth), category, cursor, size)));
    }

    // 제목 키워드 검색 - 지역 내 in-memory 필터, ?keyword=아이패드&category=전자기기
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TradeResponse>>> search(
        Authentication auth,
        @RequestParam String keyword,
        @RequestParam(required = false) String category
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(tradeService.search(uid(auth), keyword, category)));
    }

    // 내가 등록한 중고거래 목록
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TradeResponse>>> findMyItems(Authentication auth) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(tradeService.findMyItems(uid(auth))));
    }

    // 내가 좋아요한 중고거래 목록
    @GetMapping("/liked")
    public ResponseEntity<ApiResponse<List<TradeResponse>>> findLikedItems(Authentication auth) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(tradeService.findLikedItems(uid(auth))));
    }

    // 중고거래 단건 조회 - 조회할 때마다 viewCount 1 증가
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TradeResponse>> findById(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(tradeService.findById(id)));
    }

    // 중고거래 게시물 수정 - 등록자 본인만 가능, null 필드는 유지
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TradeResponse>> update(
        @PathVariable String id,
        @RequestBody @Valid TradeUpdateRequest request,
        Authentication auth
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(tradeService.update(id, uid(auth), request)));
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
