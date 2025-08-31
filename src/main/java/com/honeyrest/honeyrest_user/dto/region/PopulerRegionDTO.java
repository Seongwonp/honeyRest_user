package com.honeyrest.honeyrest_user.dto.region;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PopulerRegionDTO {
    private Integer regionId;
    private String imgUrl;
    private String name;
    private Integer level;
    private Integer parentId;
    private boolean isPopular;
}