package com.example.campusmarket.image.dto;

/**
 * 이미지 업로드 응답 DTO
 *
 * @param url 업로드된 이미지의 Firebase Storage 공개 URL
 *            형식: https://storage.googleapis.com/{bucket}/images/{UUID}.{ext}
 */
public record ImageUploadResponse(String url) {}
