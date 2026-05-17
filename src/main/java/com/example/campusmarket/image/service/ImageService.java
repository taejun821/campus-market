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
import java.util.Map;
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

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    // 파일 포맷별 매직 바이트 (MIME 타입 위변조 방지)
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
        "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "image/png",  new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
        "image/gif",  new byte[]{0x47, 0x49, 0x46}
    );
    // WebP: RIFF....WEBP (바이트 0-3: RIFF, 8-11: WEBP)
    private static final byte[] WEBP_RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_MARKER = "WEBP".getBytes(StandardCharsets.US_ASCII);

    @Value("${firebase.storage.bucket}")
    private String storageBucket;

    public ImageUploadResponse upload(MultipartFile file) throws Exception {
        if (file.isEmpty()) throw new BadRequestException("파일이 비어 있습니다.");
        if (file.getSize() > MAX_FILE_SIZE) throw new BadRequestException("파일 크기는 10MB 이하여야 합니다.");

        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException("지원하지 않는 파일 형식입니다. (jpg, png, webp, gif 허용)");
        }

        validateMagicBytes(file.getBytes(), contentType);

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

    private void validateMagicBytes(byte[] bytes, String contentType) {
        if (bytes.length < 12) throw new BadRequestException("파일이 손상되었습니다.");

        if ("image/webp".equals(contentType)) {
            if (!startsWith(bytes, WEBP_RIFF, 0) || !startsWith(bytes, WEBP_MARKER, 8)) {
                throw new BadRequestException("파일 내용이 선언된 형식과 일치하지 않습니다.");
            }
            return;
        }

        byte[] expected = MAGIC_BYTES.get(contentType);
        if (expected != null && !startsWith(bytes, expected, 0)) {
            throw new BadRequestException("파일 내용이 선언된 형식과 일치하지 않습니다.");
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix, int offset) {
        if (data.length < offset + prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) return false;
        }
        return true;
    }
}
