package com.example.campusmarket;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @PostConstruct
    public void init() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(loadCredentials())
                    .build();
            FirebaseApp.initializeApp(options);
        }
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            try (InputStream stream = new FileInputStream(credentialsPath)) {
                return GoogleCredentials.fromStream(stream);
            }
        }
        // 로컬 개발 폴백: classpath의 serviceAccountKey.json
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("serviceAccountKey.json");
        if (stream == null) {
            throw new IllegalStateException(
                    "Firebase 인증 정보를 찾을 수 없습니다. " +
                    "FIREBASE_CREDENTIALS_PATH 환경 변수를 설정하세요.");
        }
        try (InputStream s = stream) {
            return GoogleCredentials.fromStream(s);
        }
    }
}