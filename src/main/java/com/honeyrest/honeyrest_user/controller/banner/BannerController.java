package com.honeyrest.honeyrest_user.controller.banner;

import com.honeyrest.honeyrest_user.dto.banner.BannerDTO;
import com.honeyrest.honeyrest_user.entity.BannerPosition;
import com.honeyrest.honeyrest_user.response.ApiResponse;
import com.honeyrest.honeyrest_user.service.BannerService;
import com.honeyrest.honeyrest_user.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banner")
public class BannerController {

    private final BannerService bannerService;

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> createBanner(
            @RequestParam MultipartFile image,
            @RequestParam String title,
            @RequestParam String targetUrl,
            @RequestParam BannerPosition position,
            @RequestParam Integer sortOrder,
            @RequestParam Boolean isActive
    ) throws IOException {

        BannerDTO dto = BannerDTO.builder()
                .title(title)
                .targetUrl(targetUrl)
                .position(position)
                .sortOrder(sortOrder)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(3600))
                .isActive(isActive)
                .imageUrl(null) // 나중에 S3에서 받아서 세팅
                .build();

        bannerService.saveBanner(image, dto);
        return ApiResponse.ok("배너 등록 완료");
    }

    @GetMapping("/list")
    public ApiResponse<?> getActiveBanners() {
        List<BannerDTO> banners = bannerService.getBanners();
        return ApiResponse.ok(banners);
    }

    @GetMapping("/random")
    public ApiResponse<?> getRandomBanner() {
        List<BannerDTO> banners = bannerService.getBanners();

        if (banners.isEmpty()) {
            return ApiResponse.error("활성화된 배너가 없습니다");
        }

        int index = ThreadLocalRandom.current().nextInt(banners.size());
        BannerDTO selected = banners.get(index);

        return ApiResponse.ok(selected);
    }

}
