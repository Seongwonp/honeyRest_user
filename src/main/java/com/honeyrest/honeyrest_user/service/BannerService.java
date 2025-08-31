package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.banner.BannerDTO;
import com.honeyrest.honeyrest_user.entity.Banner;
import com.honeyrest.honeyrest_user.repository.BannerRepository;
import com.honeyrest.honeyrest_user.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerRepository bannerRepository;
    private final ModelMapper modelMapper;
    private final FileUploadUtil fileUploadUtil;

    public void saveBanner(MultipartFile image, BannerDTO dto) throws IOException {
        String imageUrl = "";
        try {
            imageUrl = fileUploadUtil.upload(image, "banner");
            dto.setImageUrl(imageUrl);
        } catch (Exception e) {
            log.error("이미지 업로드 실패", e);
            throw new RuntimeException("이미지 업로드 중 오류 발생");
        }
        dto.setImageUrl(imageUrl);
       log.info("dto: {}", dto);
        Banner banner = modelMapper.map(dto, Banner.class);

        bannerRepository.save(banner);
    }

    public List<BannerDTO> getBanners() {
        LocalDateTime now = LocalDateTime.now();
        List<Banner> banners = bannerRepository.findByIsActiveTrueAndStartDateBeforeAndEndDateAfterOrderBySortOrderAsc(now, now);
        return banners.stream()
                .map(banner -> BannerDTO.builder()
                        .title(banner.getTitle())
                        .imageUrl(banner.getImageUrl())
                        .targetUrl(banner.getTargetUrl())
                        .position(banner.getPosition())
                        .sortOrder(banner.getSortOrder())
                        .startDate(banner.getStartDate())
                        .endDate(banner.getEndDate())
                        .isActive(banner.getIsActive())
                        .build())
                .toList();
    }

    public BannerDTO getRandomBanner() {
        List<Banner> result = bannerRepository.findRandomBanner(LocalDateTime.now(), PageRequest.of(0, 1));
        if (result.isEmpty()) throw new IllegalStateException("활성화된 배너가 없습니다");

        Banner banner = result.get(0);

        return BannerDTO.builder()
                .bannerId(banner.getBannerId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .targetUrl(banner.getTargetUrl())
                .position(banner.getPosition())
                .sortOrder(banner.getSortOrder())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .isActive(banner.getIsActive())
                .build();
    }

}
