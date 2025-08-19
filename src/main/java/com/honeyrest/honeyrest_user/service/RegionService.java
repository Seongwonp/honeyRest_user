package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.repository.RegionRepository;
import com.honeyrest.honeyrest_user.entity.Region;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;


    @Cacheable(value = "regions", key = "T(java.util.Objects).hash(#level, #parentId, #isPopular)")
    public List<Region> findRegions(Integer level, Integer parentId, Boolean isPopular) {
        // 조건에 따라 필터링
        if (level != null && parentId != null && isPopular != null) {
            return regionRepository.findByLevelAndParentIdAndIsPopular(level, parentId, isPopular);
        } else if (level != null && parentId != null) {
            return regionRepository.findByLevelAndParentId(level, parentId);
        } else if (level != null && isPopular != null) {
            return regionRepository.findByLevelAndIsPopular(level, isPopular);
        } else if (level != null) {
            return regionRepository.findByLevel(level);
        } else {
            return regionRepository.findAll();
        }
    }
}