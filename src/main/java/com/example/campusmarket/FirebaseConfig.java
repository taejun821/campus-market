package com.example.campusmarket;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Firebase 초기화 설정
 * - Firestore (DB), Storage (이미지 저장소) 연결
 * - 테스트 환경(@Profile("!test"))에서는 실행되지 않음
 *
 * 인증 키 우선순위:
 * 1순위: 환경변수 FIREBASE_CREDENTIALS_JSON (JSON 내용 직접) → Railway 배포용
 * 2순위: 환경변수 FIREBASE_CREDENTIALS_PATH (파일 경로)     → 파일 접근 가능한 서버용
 * 3순위: src/main/resources/serviceAccountKey.json          → 로컬 개발용
 */
@Configuration
@Profile("!test")
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${firebase.storage.bucket:}")
    private String storageBucket;

    @PostConstruct
    public void init() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(loadCredentials());
            if (storageBucket != null && !storageBucket.isBlank()) {
                builder.setStorageBucket(storageBucket);
            }
            FirebaseApp.initializeApp(builder.build());
        }
    }

    @Bean
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }

    private GoogleCredentials loadCredentials() throws IOException {
        // 1순위: FIREBASE_CREDENTIALS_JSON 환경변수 (Railway 배포 시 사용)
        // Railway Variables 탭에 serviceAccountKey.json 파일 내용을 그대로 붙여넣기
        String credentialsJson = System.getenv("FIREBASE_CREDENTIALS_JSON");
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            InputStream stream = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
            return GoogleCredentials.fromStream(stream);
        }

        // 2순위: FIREBASE_CREDENTIALS_PATH 환경변수 (파일 경로)
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            try (InputStream stream = new FileInputStream(credentialsPath)) {
                return GoogleCredentials.fromStream(stream);
            }
        }

        // 3순위: resources/serviceAccountKey.json (로컬 개발용)
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("serviceAccountKey.json");
        if (stream == null) {
            throw new IllegalStateException(
                    "Firebase 인증 정보를 찾을 수 없습니다.\n" +
                    "- 로컬: src/main/resources/serviceAccountKey.json 추가\n" +
                    "- Railway: FIREBASE_CREDENTIALS_JSON 환경변수 설정");
        }
        try (InputStream s = stream) {
            return GoogleCredentials.fromStream(s);
        }
    }
}
