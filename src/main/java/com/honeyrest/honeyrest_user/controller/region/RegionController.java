package com.honeyrest.honeyrest_user.controller.region;

import com.honeyrest.honeyrest_user.dto.region.RegionDTO;
import com.honeyrest.honeyrest_user.entity.Region;
import com.honeyrest.honeyrest_user.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/region")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    public List<RegionDTO> getRegions(
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Integer parentId,
            @RequestParam(required = false) Boolean isPopular
    ) {
        List<Region> regions = regionService.findRegions(level, parentId, isPopular);
        return regions.stream()
                .map(region -> RegionDTO.builder()
                        .regionId(region.getRegionId())
                        .name(region.getName())
                        .level(region.getLevel())
                        .parentId(region.getParentId())
                        .isPopular(region.isPopular())
                        .build())
                .toList();
    }
}