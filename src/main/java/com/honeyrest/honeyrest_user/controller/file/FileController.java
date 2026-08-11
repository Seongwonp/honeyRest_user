package com.honeyrest.honeyrest_user.controller.file;

import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.review.ReviewImageRepository;
import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    // banner/event는 관리자 전용 업로드 흐름(BannerService/EventService)을 따로 쓰므로
    // 이 사용자용 엔드포인트에서는 다루지 않는다.
    private static final Set<String> ALLOWED_FOLDERS = Set.of("reviews", "profile");

    private final FileUploadUtil fileUploadUtil;
    private final UserRepository userRepository;
    private final ReviewImageRepository reviewImageRepository;

    /**
     * 이미지 업로드 (Firebase Storage)
     * @param file MultipartFile
     * @param folder 저장할 폴더명 (예: "reviews")
     * @return 업로드된 이미지 URL
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("folder") String folder) throws Exception {

        if (!ALLOWED_FOLDERS.contains(folder)) {
            throw new IllegalArgumentException("허용되지 않은 업로드 경로입니다.");
        }

        String imageUrl = fileUploadUtil.upload(file, folder);
        return ResponseEntity.ok(imageUrl);
    }

    /**
     * 이미지 삭제 (Firebase Storage)
     * 과거에는 folder/imgUrl을 그대로 신뢰해 로그인만 하면 타인의 리뷰·프로필 이미지는 물론
     * banner/event 같은 임의 경로의 파일까지 삭제할 수 있었다(P1-5). folder를 화이트리스트로
     * 제한하고, imgUrl이 실제로 호출자 소유 리소스에 연결돼 있는지 DB로 재확인한다.
     * @param folder 삭제할 폴더명
     * @param imgUrl 삭제할 이미지 URL
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteFile(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam("folder") String folder,
            @RequestParam("imgUrl") String imgUrl) {

        if (!ALLOWED_FOLDERS.contains(folder) || !ownsFile(principal.getUserId(), folder, imgUrl)) {
            throw new IllegalArgumentException("해당 파일을 삭제할 권한이 없습니다.");
        }

        fileUploadUtil.delete(folder, imgUrl);
        return ResponseEntity.noContent().build();
    }

    private boolean ownsFile(Long userId, String folder, String imgUrl) {
        if ("profile".equals(folder)) {
            return userRepository.findById(userId)
                    .map(user -> imgUrl.equals(user.getProfileImage()))
                    .orElse(false);
        }
        if ("reviews".equals(folder)) {
            return reviewImageRepository.findByImageUrl(imgUrl)
                    .map(image -> image.getReview() != null
                            && image.getReview().getUser() != null
                            && userId.equals(image.getReview().getUser().getUserId()))
                    .orElse(false);
        }
        return false;
    }
}
