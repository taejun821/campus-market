package com.example.campusmarket.image.service;

import com.example.campusmarket.common.exception.BadRequestException;
import com.example.campusmarket.image.dto.ImageUploadResponse;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * 이미지 업로드 서비스
 * 저장소: Firebase Storage (Google Cloud Storage)
 *
 * 업로드 흐름:
 * 1. 파일 유효성 검사 (비어있는지, 허용 형식인지)
 * 2. Firebase Storage에 업로드
 * 3. 파일을 공개(Public Read) 설정
 * 4. 공개 URL 반환 → Flutter에서 imageUrls 필드에 저장
 *
 * 저장 경로: images/{UUID}.{확장자}
 * 공개 URL 형식: https://storage.googleapis.com/{bucket}/images/{UUID}.{확장자}
 */
@Service
public class ImageService {

    // 허용 이미지 형식 (MIME 타입 기준)
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    @Value("${firebase.storage.bucket}")
    private String storageBucket;

    public ImageUploadResponse upload(MultipartFile file) throws Exception {
        if (file.isEmpty()) throw new BadRequestException("파일이 비어 있습니다.");

        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException("지원하지 않는 파일 형식입니다. (jpg, png, webp, gif 허용)");
        }

        // UUID로 파일명 중복 방지
        String ext = contentType.substring(contentType.lastIndexOf('/') + 1);
        String fileName = "images/" + UUID.randomUUID() + "." + ext;

        // Firebase Storage에 업로드 후 공개 접근 허용
        Blob blob = StorageClient.getInstance().bucket(storageBucket)
            .create(fileName, file.getInputStream(), contentType);
        blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));

        // URL의 슬래시(/)가 인코딩되지 않도록 +를 %20으로 변환
        String url = String.format(
            "https://storage.googleapis.com/%s/%s",
            storageBucket,
            URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20")
        );
        return new ImageUploadResponse(url);
    }
}
