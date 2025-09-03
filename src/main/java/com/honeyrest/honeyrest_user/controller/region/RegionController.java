package com.honeyrest.honeyrest_user.controller.region;

import com.honeyrest.honeyrest_user.dto.region.HotRegionDTO;
import com.honeyrest.honeyrest_user.dto.region.PopulerRegionDTO;
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

    @GetMapping("/all")
    public List<RegionDTO> getAllRegions() {
        return regionService.getAllRegions().stream()
                .map(region -> RegionDTO.builder()
                        .regionId(region.getRegionId())
                        .name(region.getName())
                        .level(region.getLevel())
                        .parentId(region.getParentId())
                        .isPopular(region.isPopular())
                        .build())
                .toList();
    }

    @GetMapping("/popular")
    public List<PopulerRegionDTO> getPopularRegions(@RequestParam(required = false) Integer level) {
        return regionService.getPopularRegions().stream()
                .map(region -> PopulerRegionDTO.builder()
                        .regionId(region.getRegionId())
                        .imgUrl(region.getImageUrl())
                        .name(region.getName())
                        .level(region.getLevel())
                        .parentId(region.getParentId())
                        .isPopular(region.isPopular())
                        .build())
                .toList();
    }


    @GetMapping("/hot")
    public List<HotRegionDTO> getHotRegions(@RequestParam(defaultValue = "8") int topN) {
        return regionService.getTopRegions(topN);
    }


    @GetMapping("/child")
    public List<RegionDTO> getRegionsByParent(
            @RequestParam Integer level,
            @RequestParam Integer parentId
    ) {
        return regionService.getRegionsByParent(level, parentId).stream()
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