package com.honeyrest.honeyrest_user.dto.region;

import com.honeyrest.honeyrest_user.entity.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegionDTO {
    private Integer regionId;
    private String name;
    private Integer level;
    private Integer parentId;
    private boolean isPopular;
    private String imageUrl;


    public static RegionDTO from(Region region) {
        return RegionDTO.builder()
                .regionId(region.getRegionId())
                .name(region.getName())
                .level(region.getLevel())
                .parentId(region.getParentId())
                .isPopular(region.isPopular())
                .imageUrl(region.getImageUrl())
                .build();
    }
}