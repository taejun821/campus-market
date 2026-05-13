package com.example.campusmarket.image.controller;

import com.example.campusmarket.common.dto.ApiResponse;
import com.example.campusmarket.image.dto.ImageUploadResponse;
import com.example.campusmarket.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 API 컨트롤러
 *
 * Base URL: /api/images
 * JWT 인증 필요
 *
 * POST /api/images/upload - 이미지 파일 업로드 (multipart/form-data)
 * 반환값: 업로드된 이미지의 공개 URL
 * 이 URL을 분실물·중고거래·커뮤니티 게시글의 imageUrls 필드에 담아 전송한다.
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    // 이미지 업로드 - form-data의 "file" 파트로 전달, 허용 형식: jpeg·png·webp·gif
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> upload(
        @RequestPart("file") MultipartFile file,
        Authentication auth
    ) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(imageService.upload(file)));
    }
}
