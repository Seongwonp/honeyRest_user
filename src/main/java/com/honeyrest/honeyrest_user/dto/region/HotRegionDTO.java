package com.honeyrest.honeyrest_user.dto.region;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HotRegionDTO {
    private String name;      // 지역명
    private long searchCount; // 조회 수
}
