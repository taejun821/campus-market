package com.example.campusmarket.community.controller;

import com.example.campusmarket.common.dto.ApiResponse;
import com.example.campusmarket.community.dto.CommentRequest;
import com.example.campusmarket.community.dto.CommentResponse;
import com.example.campusmarket.community.dto.PostRequest;
import com.example.campusmarket.community.dto.PostResponse;
import com.example.campusmarket.community.service.CommunityService;
import com.example.campusmarket.lostitem.dto.LikeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 커뮤니티 게시판 API 컨트롤러
 *
 * Base URL: /api/community
 * 모든 엔드포인트 JWT 인증 필요
 *
 * POST   /api/community                              - 게시글 작성
 * GET    /api/community                              - 게시글 목록 조회 (최신순 50개)
 * GET    /api/community/{id}                         - 게시글 단건 조회 (조회수 증가)
 * DELETE /api/community/{id}                         - 게시글 삭제 (본인만)
 * POST   /api/community/{id}/like                    - 게시글 좋아요 토글
 * POST   /api/community/{postId}/comments            - 댓글 작성
 * GET    /api/community/{postId}/comments            - 댓글 목록 조회 (오래된 순)
 * DELETE /api/community/{postId}/comments/{commentId} - 댓글 삭제 (본인만)
 */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    // 게시글 작성 - 성공 시 201 Created 반환
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
        @RequestBody @Valid PostRequest request, Authentication auth
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(communityService.createPost(request, uid(auth))));
    }

    // 게시글 목록 조회 - 최신순 정렬, 최대 50개
    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponse>>> findAllPosts() throws Exception {
        return ResponseEntity.ok(ApiResponse.success(communityService.findAllPosts()));
    }

    // 게시글 단건 조회 - 조회할 때마다 viewCount 1 증가
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> findPostById(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(communityService.findPostById(id)));
    }

    // 게시글 삭제 - 작성자 본인만 가능 (ForbiddenException 발생 가능)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable String id, Authentication auth) throws Exception {
        communityService.deletePost(id, uid(auth));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 게시글 좋아요 토글 - 현재 좋아요 상태와 총 좋아요 수 반환
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(
        @PathVariable String id, Authentication auth
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(communityService.togglePostLike(id, uid(auth))));
    }

    // 댓글 작성 - 성공 시 201 Created, commentCount 1 증가
    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
        @PathVariable String postId,
        @RequestBody @Valid CommentRequest request,
        Authentication auth
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(communityService.addComment(postId, request, uid(auth))));
    }

    // 댓글 목록 조회 - 작성 시각 오름차순 (오래된 댓글이 위에)
    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(@PathVariable String postId) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(communityService.getComments(postId)));
    }

    // 댓글 삭제 - 작성자 본인만 가능, 삭제 후 commentCount 1 감소
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
        @PathVariable String postId,
        @PathVariable String commentId,
        Authentication auth
    ) throws Exception {
        communityService.deleteComment(postId, commentId, uid(auth));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // JWT principal에서 uid 추출
    private String uid(Authentication auth) {
        return (String) auth.getPrincipal();
    }
}
