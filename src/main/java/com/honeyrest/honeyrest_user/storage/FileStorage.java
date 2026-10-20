package com.honeyrest.honeyrest_user.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드 파일 저장소. {@code app.storage.type} 으로 {@link LocalFileStorage}(기본)/{@link FirebaseFileStorage} 를 고른다.
 * 두 구현 모두 업로드 전에 {@link FileValidator} 로 검증한다.
 */
public interface FileStorage {

    /**
     * 파일을 업로드하고 접근 가능한 URL을 반환한다.
     *
     * @param file   업로드할 파일
     * @param folder 저장 폴더명 (예: "reviews", "profile", "banner", "event")
     * @return 저장된 파일에 접근할 수 있는 URL
     */
    String upload(MultipartFile file, String folder) throws Exception;

    /**
     * URL이 가리키는 파일을 삭제한다.
     * 보안상 {@code folder} 하위에 있는 파일만 삭제하며, 그 외 URL은 조용히 무시한다.
     *
     * @param folder 삭제를 허용할 폴더명
     * @param url    {@link #upload} 가 반환했던 URL
     */
    void delete(String folder, String url);

    /**
     * 폴더명 검증: 경로 조작(../ 등)을 막기 위해 영문/숫자/-/_ 만 허용한다.
     * 실제 규칙은 {@link FileValidator#requireSafeFolder} 한 곳에 둔다.
     */
    static String requireSafeFolder(String folder) {
        return FileValidator.requireSafeFolder(folder);
    }
}
