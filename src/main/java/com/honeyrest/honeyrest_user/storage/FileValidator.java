package com.honeyrest.honeyrest_user.storage;

import com.honeyrest.honeyrest_user.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 업로드 파일 공통 검증 (LocalFileStorage / FirebaseFileStorage 가 함께 사용).
 * <ol>
 *     <li>폴더명: 영문/숫자/-/_ 만 허용 (경로 조작 차단)</li>
 *     <li>크기: {@code app.storage.max-file-size} (기본 5MB) 초과 시 413</li>
 *     <li>확장자: jpg/jpeg/png/gif/webp 만 허용</li>
 *     <li>내용 스니핑: 파일 앞부분 매직 바이트가 확장자와 같은 이미지 형식인지 확인
 *         (확장자만 바꾼 HTML/SVG/실행 파일이 /uploads/** 로 공개 서빙되는 것을 막는다)</li>
 * </ol>
 */
@Component
public class FileValidator {

    /** 확장자 → 매직 바이트로 판별한 형식 */
    private static final Map<String, String> EXTENSION_TO_TYPE = Map.of(
            "jpg", "jpeg",
            "jpeg", "jpeg",
            "png", "png",
            "gif", "gif",
            "webp", "webp"
    );

    public static final Set<String> ALLOWED_EXTENSIONS = EXTENSION_TO_TYPE.keySet();

    private static final int SNIFF_LENGTH = 12;

    private final long maxBytes;

    public FileValidator(@Value("${app.storage.max-file-size:5MB}") DataSize maxFileSize) {
        this.maxBytes = maxFileSize.toBytes();
    }

    /** 폴더명 검증: 경로 조작(../, /, \ 등)을 막기 위해 영문/숫자/-/_ 만 허용한다. */
    public static String requireSafeFolder(String folder) {
        if (folder == null || !folder.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("허용되지 않은 폴더명입니다: " + folder);
        }
        return folder;
    }

    /**
     * 파일을 검증하고 저장에 쓸 소문자 확장자를 반환한다.
     * 원본 파일명은 확장자 판별에만 쓰며 저장 경로에는 절대 넣지 않는다.
     */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }
        if (file.getSize() > maxBytes) {
            throw new ApiException("파일 크기는 " + DataSize.ofBytes(maxBytes).toMegabytes() + "MB 를 넘을 수 없습니다.",
                    HttpStatus.PAYLOAD_TOO_LARGE);
        }

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String lowerExt = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        String expectedType = EXTENSION_TO_TYPE.get(lowerExt);
        if (expectedType == null) {
            throw new IllegalArgumentException("허용되지 않은 파일 형식입니다: " + ext);
        }

        String detected = detectImageType(readHeader(file));
        if (!expectedType.equals(detected)) {
            throw new IllegalArgumentException("파일 내용이 확장자(" + lowerExt + ")와 일치하는 이미지가 아닙니다.");
        }
        return lowerExt;
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(SNIFF_LENGTH);
        } catch (IOException e) {
            throw new IllegalArgumentException("파일을 읽을 수 없습니다.");
        }
    }

    /** 매직 바이트로 이미지 형식을 판별한다. 알 수 없으면 null. */
    static String detectImageType(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return "png";
        }
        if (h.length >= 6 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8'
                && (h[4] == '7' || h[4] == '9') && h[5] == 'a') {
            return "gif";
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
