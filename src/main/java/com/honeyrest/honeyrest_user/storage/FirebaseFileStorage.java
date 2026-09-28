package com.honeyrest.honeyrest_user.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Firebase(Google Cloud) Storage 기반 파일 저장소.
 * <p>
 * {@code app.storage.type=firebase} 일 때만 빈으로 등록된다.
 * 서비스 계정 키 파일 경로({@code fire.base.secretKey}, classpath 기준)와
 * 버킷/프로젝트 ID({@code app.storage.firebase.*})는 프로퍼티로 주입한다.
 * (기존 util/FileUploadUtil 로직을 옮겨 온 것이다.)
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "firebase")
public class FirebaseFileStorage implements FileStorage {

    private Storage storage;

    private final FileValidator fileValidator;

    public FirebaseFileStorage(FileValidator fileValidator) {
        this.fileValidator = fileValidator;
    }

    @Value("${fire.base.secretKey}")
    private String firebaseSecretKey;

    @Value("${app.storage.firebase.bucket}")
    private String bucket;

    @Value("${app.storage.firebase.project-id}")
    private String projectId;

    @PostConstruct
    void initFirebaseStorage() throws Exception {
        try (InputStream in = new ClassPathResource(firebaseSecretKey).getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(in)
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

            storage = StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build()
                    .getService();
        }
    }

    @Override
    public String upload(MultipartFile file, String folder) throws Exception {
        FileValidator.requireSafeFolder(folder);
        String ext = fileValidator.validate(file);

        // 원본 파일명에는 '/', '..' 등이 들어올 수 있으므로 blob 이름에 쓰지 않고 UUID + 검증된 확장자만 쓴다.
        String filename = UUID.randomUUID() + "." + ext;
        String blobName = folder + "/" + filename;

        // BlobInfo 생성: 파일 메타데이터 포함
        BlobInfo blobInfo = BlobInfo.newBuilder(bucket, blobName)
                // 클라이언트가 보낸 Content-Type 대신 검증된 확장자로 결정한다.
                .setContentType("image/" + ("jpg".equals(ext) ? "jpeg" : ext))
                .build();

        // 파일 업로드
        storage.create(blobInfo, file.getBytes());

        // 다운로드 토큰 생성 (Firebase에서 이미지 접근 시 필요)
        String downloadToken = UUID.randomUUID().toString();
        storage.update(blobInfo.toBuilder()
                .setMetadata(Map.of("firebaseStorageDownloadTokens", downloadToken))
                .build());

        // 최종 접근 가능한 이미지 URL 반환
        return "https://firebasestorage.googleapis.com/v0/b/" + bucket + "/o/" +
                URLEncoder.encode(blobName, StandardCharsets.UTF_8) +
                "?alt=media&token=" + downloadToken;
    }

    @Override
    public void delete(String folder, String url) {
        String blobName = extractBlobName(url);

        // 보안 체크: 지정된 폴더 안에 있는 파일만 삭제 허용
        if (blobName != null && blobName.startsWith(folder + "/")) {
            storage.delete(bucket, blobName);
        }
    }

    /**
     * Firebase 이미지 URL에서 blobName 추출
     * 예: https://.../o/reviews%2Fabc.jpg?alt=media → reviews/abc.jpg
     * Firebase 형식이 아닌 URL이면 null을 반환한다.
     */
    private String extractBlobName(String url) {
        if (url == null) {
            return null;
        }
        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
        int marker = decoded.indexOf("/o/");
        int end = decoded.indexOf("?alt=media");
        if (marker < 0 || end < 0 || end < marker + 3) {
            return null;
        }
        return decoded.substring(marker + 3, end);
    }
}
