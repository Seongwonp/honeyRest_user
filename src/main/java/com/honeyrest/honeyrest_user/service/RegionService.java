package com.honeyrest.honeyrest_user.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.honeyrest_user.repository.RegionRepository;
import com.honeyrest.honeyrest_user.entity.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class RegionService {

    private final RegionRepository regionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public List<Region> getAllRegions() {
        String key = "regions:all";
        Object raw = redisTemplate.opsForValue().get(key);
        List<Region> cached = raw != null
                ? objectMapper.convertValue(raw, new TypeReference<List<Region>>() {})
                : null;

        if (cached != null && !cached.isEmpty()) {
            log.info("🚀 전체 지역 캐시 HIT");
            return cached;
        }

        List<Region> regions = regionRepository.findAll();
        redisTemplate.opsForValue().set(key, regions, Duration.ofHours(12));
        log.info("📦 전체 지역 캐시 저장");
        return regions;
    }

    public List<Region> getPopularRegions() {
        String key = "regions:popular";
        Object raw = redisTemplate.opsForValue().get(key);
        List<Region> cached = raw != null
                ? objectMapper.convertValue(raw, new TypeReference<List<Region>>() {})
                : null;

        if (cached != null && !cached.isEmpty()) {
            log.info("🚀 인기 지역 캐시 HIT");
            return cached;
        }

        List<Region> regions = regionRepository.findByIsPopularOrderByRegionIdAsc(true);
        redisTemplate.opsForValue().set(key, regions, Duration.ofHours(12));
        log.info("📦 인기 지역 캐시 저장");
        return regions;
    }

    public List<Region> getRegionsByParent(Integer level, Integer parentId) {
        String key = String.format("regions:child:%s:%s",
                level != null ? level : "any",
                parentId != null ? parentId : "any"
        );

        Object raw = redisTemplate.opsForValue().get(key);
        List<Region> cached = raw != null
                ? objectMapper.convertValue(raw, new TypeReference<List<Region>>() {})
                : null;

        if (cached != null && !cached.isEmpty()) {
            log.info("🚀 하위 지역 캐시 HIT");
            return cached;
        }

        List<Region> regions = regionRepository.findByLevelAndParentId(level, parentId);
        redisTemplate.opsForValue().set(key, regions, Duration.ofHours(12));
        log.info("📦 하위 지역 캐시 저장");
        return regions;
    }
}