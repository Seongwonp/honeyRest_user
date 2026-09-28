package com.honeyrest.honeyrest_user.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 로컬 파일 저장소: 업로드 경로/URL 규칙과 삭제 범위 제한을 검증한다.
 */
class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage storage;

    /** 최소 PNG 시그니처 + 임의 바이트 */
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    @BeforeEach
    void setUp() {
        storage = new LocalFileStorage(tempDir.toString(), "/uploads", new FileValidator(DataSize.ofMegabytes(5)));
    }

    @Test
    void 업로드하면_폴더_하위에_저장되고_uploads_URL을_반환한다() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.PNG", "image/png", PNG);

        String url = storage.upload(file, "reviews");

        assertThat(url).startsWith("/uploads/reviews/").endsWith(".png");
        Path saved = tempDir.resolve(url.substring("/uploads/".length()));
        assertThat(Files.readAllBytes(saved)).containsExactly(PNG);
    }

    @Test
    void 이미지가_아닌_확장자는_거부된다() {
        MockMultipartFile file = new MockMultipartFile("file", "evil.svg", "image/svg+xml", new byte[]{1});

        assertThatThrownBy(() -> storage.upload(file, "reviews"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 경로_조작_폴더명은_거부된다() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG);

        assertThatThrownBy(() -> storage.upload(file, "../etc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 삭제는_지정_폴더_안의_파일만_지운다() throws Exception {
        String url = storage.upload(new MockMultipartFile("file", "a.png", "image/png", PNG), "profile");
        Path saved = tempDir.resolve(url.substring("/uploads/".length()));

        // 다른 폴더명으로 요청하면 무시
        storage.delete("reviews", url);
        assertThat(saved).exists();

        // 폴더 밖으로 벗어나는 경로는 무시
        Files.writeString(tempDir.resolve("secret.txt"), "x");
        storage.delete("profile", "/uploads/profile/../secret.txt");
        assertThat(tempDir.resolve("secret.txt")).exists();

        storage.delete("profile", url);
        assertThat(saved).doesNotExist();
    }

    @Test
    void 확장자만_png로_바꾼_HTML은_저장되지_않는다() {
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png",
                "<html><script>alert(1)</script></html>".getBytes());

        assertThatThrownBy(() -> storage.upload(file, "reviews"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(tempDir.resolve("reviews")).doesNotExist();
    }
}
