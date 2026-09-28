package com.honeyrest.honeyrest_user.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 등 업로드 파일 저장소 추상화.
 * <p>
 * 구현체는 {@code app.storage.type} 프로퍼티로 선택한다.
 * <ul>
 *     <li>{@code local} (기본값) — {@link LocalFileStorage}: 로컬 디스크에 저장하고 {@code /uploads/**} 로 서빙</li>
 *     <li>{@code firebase} — {@link FirebaseFileStorage}: Firebase(Google Cloud) Storage 버킷에 저장</li>
 * </ul>
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
     */
    static String requireSafeFolder(String folder) {
        if (folder == null || !folder.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("허용되지 않은 폴더명입니다: " + folder);
        }
        return folder;
    }
}
