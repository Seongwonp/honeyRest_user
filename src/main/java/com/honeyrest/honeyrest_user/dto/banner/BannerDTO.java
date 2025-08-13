package com.honeyrest.honeyrest_user.dto.banner;

import com.honeyrest.honeyrest_user.entity.BannerPosition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "배너 등록 요청 DTO")
public class BannerDTO {

    @Schema(description = "배너 제목", example = "여름 할인 배너")
    private String title;

    @Schema(description = "배너 이미지 URL", example = "https://s3.filebase.com/honeyrest/static/bg1.jpg")
    private String imageUrl;

    @Schema(description = "배너 클릭 시 이동할 URL", example = "/event/summer")
    private String targetUrl;

    @Schema(description = "배너 위치", example = "TOP")
    private BannerPosition position;

    @Schema(description = "배너 정렬 순서", example = "1")
    private Integer sortOrder;

    @Schema(description = "배너 시작 날짜", example = "2025-08-13T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "배너 종료 날짜", example = "2025-08-20T00:00:00")
    private LocalDateTime endDate;

    @Schema(description = "배너 활성화 여부", example = "true")
    private Boolean isActive;
}