package com.honeyrest.honeyrest_user.dto.region;

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
}