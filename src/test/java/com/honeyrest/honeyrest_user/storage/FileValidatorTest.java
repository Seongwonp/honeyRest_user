package com.honeyrest.honeyrest_user.storage;

import com.honeyrest.honeyrest_user.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 업로드 공통 검증: 크기 · 확장자 · 매직 바이트 · 폴더명.
 */
class FileValidatorTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 1, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P'};

    private final FileValidator validator = new FileValidator(DataSize.ofKilobytes(1));

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }

    @Test
    void 허용된_4개_형식은_매직_바이트가_맞으면_통과한다() {
        assertThat(validator.validate(file("a.JPG", JPEG))).isEqualTo("jpg");
        assertThat(validator.validate(file("a.jpeg", JPEG))).isEqualTo("jpeg");
        assertThat(validator.validate(file("a.png", PNG))).isEqualTo("png");
        assertThat(validator.validate(file("a.gif", GIF))).isEqualTo("gif");
        assertThat(validator.validate(file("a.webp", WEBP))).isEqualTo("webp");
    }

    @Test
    void 확장자와_내용이_다르면_거부한다() {
        // PNG 내용인데 jpg 확장자
        assertThatThrownBy(() -> validator.validate(file("a.jpg", PNG)))
                .isInstanceOf(IllegalArgumentException.class);
        // 이미지가 아닌 내용
        assertThatThrownBy(() -> validator.validate(file("a.gif", "<svg onload=alert(1)>".getBytes())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 허용되지_않은_확장자와_빈_파일은_거부한다() {
        assertThatThrownBy(() -> validator.validate(file("a.svg", PNG)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate(file("noext", PNG)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate(file("a.png", new byte[0])))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 최대_크기를_넘으면_413() {
        byte[] big = Arrays.copyOf(PNG, 2048);

        assertThatThrownBy(() -> validator.validate(file("a.png", big)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void 폴더명에_경로_문자가_있으면_거부한다() {
        assertThat(FileValidator.requireSafeFolder("reviews")).isEqualTo("reviews");
        for (String bad : new String[]{"../etc", "a/b", "a\\b", "..", "", " reviews", null}) {
            assertThatThrownBy(() -> FileValidator.requireSafeFolder(bad))
                    .as("folder=%s", bad)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
