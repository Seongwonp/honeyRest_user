package com.honeyrest.honeyrest_user.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

/**
 * Firebase Admin SDK 초기화.
 * <p>
 * {@code app.storage.type=firebase} 일 때만 활성화된다. 기본값(local)에서는
 * 서비스 계정 키 파일 없이도 애플리케이션이 기동된다.
 */
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "firebase")
public class FirebaseConfig {

    @Value("${fire.base.secretKey}")
    private String firebaseSecretKey;

    @Value("${app.storage.firebase.bucket}")
    private String bucket;

    @PostConstruct
    public void init() throws Exception {
        try (InputStream serviceAccount = getClass().getClassLoader()
                .getResourceAsStream(firebaseSecretKey)) {

            if (serviceAccount == null) {
                throw new IllegalStateException("Firebase 서비스 계정 키 파일을 찾을 수 없습니다.");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setStorageBucket(bucket)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        }
    }
}
