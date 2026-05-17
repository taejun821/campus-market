package com.example.campusmarket.lostitem.controller;

import com.example.campusmarket.common.dto.ApiResponse;
import com.example.campusmarket.lostitem.dto.LikeResponse;
import com.example.campusmarket.lostitem.dto.LostItemRequest;
import com.example.campusmarket.lostitem.dto.LostItemResponse;
import com.example.campusmarket.lostitem.dto.LostItemStatusRequest;
import com.example.campusmarket.lostitem.dto.LostItemUpdateRequest;
import com.example.campusmarket.lostitem.service.LostItemService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 분실물 API 컨트롤러
 *
 * Base URL: /api/lost-items
 * 모든 엔드포인트 JWT 인증 필요
 *
 * POST   /api/lost-items                - 분실물 등록
 * GET    /api/lost-items                - 분실물 목록 조회 (최신순)
 * GET    /api/lost-items/my             - 내가 등록한 분실물 목록
 * GET    /api/lost-items/{id}           - 분실물 단건 조회 (조회수 증가)
 * PUT    /api/lost-items/{id}           - 분실물 수정 (본인만)
 * PATCH  /api/lost-items/{id}/status    - 분실물 상태 변경 LOST/FOUND (본인만)
 * DELETE /api/lost-items/{id}           - 분실물 삭제 (본인만)
 * POST   /api/lost-items/{id}/like      - 좋아요 토글
 */
@RestController
@RequestMapping("/api/lost-items")
@RequiredArgsConstructor
public class LostItemController {

    private final LostItemService lostItemService;

    // 분실물 등록 - 성공 시 201 Created 반환
    @PostMapping
    public ResponseEntity<ApiResponse<LostItemResponse>> create(
        @RequestBody @Valid LostItemRequest request,
        Authentication auth
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(lostItemService.create(request, getUid(auth))));
    }

    // 분실물 목록 조회 - 로그인 유저와 동일 지역 게시물만, 최신순 정렬
    @GetMapping
    public ResponseEntity<ApiResponse<List<LostItemResponse>>> findAll(Authentication auth) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(lostItemService.findAll(getUid(auth))));
    }

    // 내가 등록한 분실물 목록
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LostItemResponse>>> findMyItems(Authentication auth) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(lostItemService.findMyItems(getUid(auth))));
    }

    // 분실물 단건 조회 - 조회할 때마다 viewCount 1 증가
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LostItemResponse>> findById(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(lostItemService.findById(id)));
    }

    // 분실물 수정 - 등록자 본인만 가능, null 필드는 유지
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LostItemResponse>> update(
        @PathVariable String id,
        @RequestBody @Valid LostItemUpdateRequest request,
        Authentication auth
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(lostItemService.update(id, getUid(auth), request)));
    }

    // 분실물 상태 변경 - 등록자 본인만 가능 (LOST: 분실중 / FOUND: 찾았음)
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<LostItemResponse>> updateStatus(
        @PathVariable String id,
        @RequestBody @Valid LostItemStatusRequest request,
        Authentication auth
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(lostItemService.updateStatus(id, getUid(auth), request)));
    }

    // 분실물 삭제 - 등록자 본인만 가능 (ForbiddenException 발생 가능)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
        @PathVariable String id,
        Authentication auth
    ) throws Exception {
        lostItemService.delete(id, getUid(auth));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 좋아요 토글 - 좋아요 상태이면 취소, 아니면 추가 (결과 상태와 현재 좋아요 수 반환)
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(
        @PathVariable String id, Authentication auth
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(lostItemService.toggleLike(id, getUid(auth))));
    }

    // JWT principal에서 uid 추출
    private String getUid(Authentication auth) {
        return (String) auth.getPrincipal();
    }
}
