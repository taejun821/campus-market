package com.example.campusmarket.community.service;

import com.example.campusmarket.common.exception.ForbiddenException;
import com.example.campusmarket.common.exception.NotFoundException;
import com.example.campusmarket.community.dto.CommentRequest;
import com.example.campusmarket.community.dto.CommentResponse;
import com.example.campusmarket.community.dto.PostRequest;
import com.example.campusmarket.community.dto.PostResponse;
import com.example.campusmarket.lostitem.dto.LikeResponse;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 커뮤니티 게시판 서비스
 * Firestore 컬렉션:
 * - "community_posts"    : 게시글
 * - "community_comments" : 댓글
 *
 * 주요 기능:
 * - 게시글 CRUD, 좋아요 토글 (트랜잭션), 조회수 증가
 * - 댓글 작성·조회·삭제 (삭제 시 게시글 commentCount 자동 감소)
 */
@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final String POSTS = "community_posts";
    private static final String COMMENTS = "community_comments";

    private final Firestore firestore;

    // 게시글 작성 - 작성자 이름·대학교를 users 컬렉션에서 조회해 함께 저장
    public PostResponse createPost(PostRequest request, String uid) throws Exception {
        DocumentSnapshot user = firestore.collection("users").document(uid).get().get();
        String username = user.getString("name");
        String university = user.getString("university");

        Map<String, Object> data = new HashMap<>();
        data.put("title", request.title());
        data.put("content", request.content());
        data.put("imageUrls", request.imageUrls() != null ? request.imageUrls() : List.of());
        data.put("userid", uid);
        data.put("username", username);
        data.put("university", university);
        data.put("viewCount", 0L);
        data.put("likeCount", 0L);
        data.put("commentCount", 0L);
        data.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference ref = firestore.collection(POSTS).document();
        ref.set(data).get();

        return new PostResponse(
            ref.getId(), request.title(), request.content(), request.imageUrls(),
            uid, username, university, 0L, 0L, 0L, null
        );
    }

    // 게시글 목록 조회 - 최신순 정렬, 최대 50개 반환
    public List<PostResponse> findAllPosts() throws Exception {
        return firestore.collection(POSTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get().get()
            .getDocuments()
            .stream()
            .map(this::toPostResponse)
            .toList();
    }

    // 게시글 단건 조회 - 조회할 때마다 viewCount 1 증가
    public PostResponse findPostById(String id) throws Exception {
        DocumentReference ref = firestore.collection(POSTS).document(id);
        DocumentSnapshot doc = ref.get().get();
        if (!doc.exists()) throw new NotFoundException("존재하지 않는 게시글입니다.");
        ref.update("viewCount", FieldValue.increment(1)).get();
        return toPostResponse(doc);
    }

    // 게시글 삭제 - 작성자 본인만 가능
    public void deletePost(String id, String uid) throws Exception {
        DocumentSnapshot doc = firestore.collection(POSTS).document(id).get().get();
        if (!doc.exists()) throw new NotFoundException("존재하지 않는 게시글입니다.");
        if (!uid.equals(doc.getString("userid"))) throw new ForbiddenException("본인이 작성한 게시글만 삭제할 수 있습니다.");
        firestore.collection(POSTS).document(id).delete().get();
    }

    // 게시글 좋아요 토글 - likes 컬렉션 "community_{id}_{uid}" 문서로 좋아요 여부 저장, 트랜잭션으로 동시성 처리
    public LikeResponse togglePostLike(String id, String uid) throws Exception {
        DocumentReference postRef = firestore.collection(POSTS).document(id);
        if (!postRef.get().get().exists()) throw new NotFoundException("존재하지 않는 게시글입니다.");

        DocumentReference likeRef = firestore.collection("likes").document("community_" + id + "_" + uid);
        boolean[] liked = {false};
        long[] likeCount = {0};

        firestore.runTransaction(transaction -> {
            DocumentSnapshot likeDoc = transaction.get(likeRef).get();
            DocumentSnapshot postDoc = transaction.get(postRef).get();
            Long current = postDoc.getLong("likeCount");
            if (current == null) current = 0L;

            if (likeDoc.exists()) {
                transaction.delete(likeRef);
                transaction.update(postRef, "likeCount", FieldValue.increment(-1));
                liked[0] = false;
                likeCount[0] = Math.max(0, current - 1);
            } else {
                transaction.set(likeRef, Map.of(
                    "targetId", id, "type", "community",
                    "userId", uid, "createdAt", FieldValue.serverTimestamp()
                ));
                transaction.update(postRef, "likeCount", FieldValue.increment(1));
                liked[0] = true;
                likeCount[0] = current + 1;
            }
            return null;
        }).get();

        return new LikeResponse(liked[0], likeCount[0]);
    }

    // 댓글 작성 - 게시글 존재 확인 후 저장, 게시글 commentCount 1 증가
    public CommentResponse addComment(String postId, CommentRequest request, String uid) throws Exception {
        DocumentSnapshot postDoc = firestore.collection(POSTS).document(postId).get().get();
        if (!postDoc.exists()) throw new NotFoundException("존재하지 않는 게시글입니다.");

        DocumentSnapshot user = firestore.collection("users").document(uid).get().get();
        String username = user.getString("name");

        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("content", request.content());
        data.put("userid", uid);
        data.put("username", username);
        data.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference ref = firestore.collection(COMMENTS).document();
        ref.set(data).get();
        firestore.collection(POSTS).document(postId).update("commentCount", FieldValue.increment(1)).get();

        return new CommentResponse(ref.getId(), postId, request.content(), uid, username, null);
    }

    // 댓글 목록 조회 - 게시글 존재 확인 후 작성 시각 오름차순 반환
    public List<CommentResponse> getComments(String postId) throws Exception {
        if (!firestore.collection(POSTS).document(postId).get().get().exists()) {
            throw new NotFoundException("존재하지 않는 게시글입니다.");
        }
        return firestore.collection(COMMENTS)
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get().get()
            .getDocuments()
            .stream()
            .map(doc -> {
                Timestamp createdAt = doc.getTimestamp("createdAt");
                return new CommentResponse(
                    doc.getId(), doc.getString("postId"), doc.getString("content"),
                    doc.getString("userid"), doc.getString("username"),
                    createdAt != null ? createdAt.toDate().getTime() : null
                );
            })
            .toList();
    }

    // 댓글 삭제 - 작성자 본인만 가능, 삭제 후 게시글 commentCount 1 감소
    public void deleteComment(String postId, String commentId, String uid) throws Exception {
        DocumentSnapshot doc = firestore.collection(COMMENTS).document(commentId).get().get();
        if (!doc.exists()) throw new NotFoundException("존재하지 않는 댓글입니다.");
        if (!uid.equals(doc.getString("userid"))) throw new ForbiddenException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        firestore.collection(COMMENTS).document(commentId).delete().get();
        firestore.collection(POSTS).document(postId).update("commentCount", FieldValue.increment(-1)).get();
    }

    // Firestore 문서 → PostResponse 변환 (null 안전 처리 포함)
    @SuppressWarnings("unchecked")
    private PostResponse toPostResponse(DocumentSnapshot doc) {
        Timestamp createdAt = doc.getTimestamp("createdAt");
        Long viewCount = doc.getLong("viewCount");
        Long likeCount = doc.getLong("likeCount");
        Long commentCount = doc.getLong("commentCount");
        return new PostResponse(
            doc.getId(), doc.getString("title"), doc.getString("content"),
            (List<String>) doc.get("imageUrls"),
            doc.getString("userid"), doc.getString("username"), doc.getString("university"),
            viewCount != null ? viewCount : 0L,
            likeCount != null ? likeCount : 0L,
            commentCount != null ? commentCount : 0L,
            createdAt != null ? createdAt.toDate().getTime() : null
        );
    }
}
