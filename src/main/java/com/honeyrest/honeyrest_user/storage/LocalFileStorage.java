package com.honeyrest.honeyrest_user.storage;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 로컬 디스크 기반 파일 저장소 (기본값).
 * <p>
 * {@code app.storage.type=local} 이거나 값이 없을 때 빈으로 등록된다.
 * 파일은 {@code app.storage.local.dir}(기본 ./uploads) 아래 {@code {folder}/{uuid}.{확장자}} 로 저장되고,
 * {@code config/WebConfig} 의 {@code /uploads/**} 리소스 핸들러로 서빙된다.
 * 반환 URL은 {@code app.storage.local.public-url-prefix}(기본 /uploads) + "/{folder}/{파일명}" 이다.
 */
@Log4j2
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final Path baseDir;
    private final String publicUrlPrefix;
    private final FileValidator fileValidator;

    public LocalFileStorage(
            @Value("${app.storage.local.dir:./uploads}") String dir,
            @Value("${app.storage.local.public-url-prefix:/uploads}") String publicUrlPrefix,
            FileValidator fileValidator) {
        this.fileValidator = fileValidator;
        this.baseDir = Paths.get(dir).toAbsolutePath().normalize();
        this.publicUrlPrefix = publicUrlPrefix.endsWith("/")
                ? publicUrlPrefix.substring(0, publicUrlPrefix.length() - 1)
                : publicUrlPrefix;
    }

    @Override
    public String upload(MultipartFile file, String folder) throws IOException {
        FileValidator.requireSafeFolder(folder);

        // 원본 파일명은 경로 조작 위험이 있으므로 확장자만 사용한다.
        // 업로드 파일은 /uploads/** 로 공개 서빙되므로 크기·확장자·매직 바이트를 FileValidator 로 검증한다.
        String lowerExt = fileValidator.validate(file);
        String filename = UUID.randomUUID() + "." + lowerExt;

        Path dir = baseDir.resolve(folder);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(baseDir)) {
            throw new IllegalArgumentException("잘못된 저장 경로입니다.");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return publicUrlPrefix + "/" + folder + "/" + filename;
    }

    @Override
    public void delete(String folder, String url) {
        if (url == null || folder == null) {
            return;
        }
        // 절대 URL(http://host/uploads/...)로 저장된 경우도 처리할 수 있도록 prefix 위치를 찾는다.
        String marker = publicUrlPrefix + "/" + folder + "/";
        int idx = url.indexOf(marker);
        if (idx < 0) {
            return; // 이 저장소가 발급한 URL이 아니거나 다른 폴더의 파일
        }
        String relative = url.substring(idx + publicUrlPrefix.length() + 1);
        int query = relative.indexOf('?');
        if (query >= 0) {
            relative = relative.substring(0, query);
        }

        Path target = baseDir.resolve(relative).normalize();
        // 보안 체크: 지정 폴더 밖의 파일은 삭제하지 않는다.
        if (!target.startsWith(baseDir.resolve(folder))) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("로컬 파일 삭제 실패: {}", target, e);
        }
    }

}
