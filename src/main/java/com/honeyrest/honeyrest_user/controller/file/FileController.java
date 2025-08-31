package com.honeyrest.honeyrest_user.controller.file;

import com.honeyrest.honeyrest_user.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileUploadUtil fileUploadUtil;

    /**
     * 이미지 업로드 (Firebase Storage)
     * @param file MultipartFile
     * @param folder 저장할 폴더명 (예: "reviews")
     * @return 업로드된 이미지 URL
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folder") String folder) throws Exception {

        String imageUrl = fileUploadUtil.upload(file, folder);
        return ResponseEntity.ok(imageUrl);
    }

    /**
     * 이미지 삭제 (Firebase Storage)
     * @param folder 삭제할 폴더명
     * @param imgUrl 삭제할 이미지 URL
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteFile(
            @RequestParam("folder") String folder,
            @RequestParam("imgUrl") String imgUrl) {

        fileUploadUtil.delete(folder, imgUrl);
        return ResponseEntity.noContent().build();
    }
}