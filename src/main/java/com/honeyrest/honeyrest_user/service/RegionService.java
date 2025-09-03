package com.honeyrest.honeyrest_user.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.honeyrest_user.dto.region.HotRegionDTO;
import com.honeyrest.honeyrest_user.dto.region.RegionDTO;
import com.honeyrest.honeyrest_user.repository.RegionRepository;
import com.honeyrest.honeyrest_user.entity.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class RegionService {

    private final RegionRepository regionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public List<RegionDTO> getAllRegions() {
        String key = "regions:all";
        Object raw = redisTemplate.opsForValue().get(key);
        List<RegionDTO> cached = raw != null
                ? objectMapper.convertValue(raw, new TypeReference<List<RegionDTO>>() {})
                : null;

        if (cached != null && !cached.isEmpty()) {
            log.info("🚀 전체 지역 캐시 HIT");
            return cached;
        }

        List<RegionDTO> regions = regionRepository.findAll().stream()
                .map(RegionDTO::from)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(key, regions, Duration.ofHours(12));
        log.info("📦 전체 지역 캐시 저장");
        return regions;
    }

    public List<RegionDTO> getPopularRegions() {
        String key = "regions:popular";
        Object raw = redisTemplate.opsForValue().get(key);
        List<RegionDTO> cached = raw != null
                ? objectMapper.convertValue(raw, new TypeReference<List<RegionDTO>>() {})
                : null;

        if (cached != null && !cached.isEmpty()) {
            log.info("🚀 인기 지역 캐시 HIT");
            return cached;
        }

        List<RegionDTO> regions = regionRepository.findByIsPopularOrderByRegionIdAsc(true).stream()
                .map(RegionDTO::from)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(key, regions, Duration.ofHours(12));
        log.info("📦 인기 지역 캐시 저장");
        return regions;
    }

    public List<RegionDTO> getRegionsByParent(Integer level, Integer parentId) {
        String key = String.format("regions:child:%s:%s",
                level != null ? level : "any",
                parentId != null ? parentId : "any"
        );

        Object raw = redisTemplate.opsForValue().get(key);
        List<RegionDTO> cached = raw != null
                ? objectMapper.convertValue(raw, new TypeReference<List<RegionDTO>>() {})
                : null;

        if (cached != null && !cached.isEmpty()) {
            log.info("🚀 하위 지역 캐시 HIT");
            return cached;
        }

        List<RegionDTO> regions = regionRepository.findByLevelAndParentId(level, parentId).stream()
                .map(RegionDTO::from)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(key, regions, Duration.ofHours(12));
        log.info("📦 하위 지역 캐시 저장");
        return regions;
    }

    // 인기 지역 조회 수 기록
    public void recordRegionSearch(String regionName) {
        String key = "regions:popular:count"; // 조회 수 기록용 Sorted Set
        redisTemplate.opsForZSet().incrementScore(key, regionName, 1);
        log.info("📈 지역 검색 기록: {} (점수 증가)", regionName);
    }

    // 상위 N개 인기 지역 조회
    public List<HotRegionDTO> getTopRegions(int topN) {
        log.info("🔥 인기 지역 조회 요청: topN={}", topN);
        String key = "regions:popular:count";
        Set<ZSetOperations.TypedTuple<Object>> rawTopRegions = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, topN - 1);

        if (rawTopRegions == null || rawTopRegions.isEmpty()) {
            log.info("🔥 인기 지역 상위 {}개 결과: 0", topN);
            return Collections.emptyList();
        }

        log.info("🔥 인기 지역 상위 {}개 결과: {}", topN, rawTopRegions.size());

        return rawTopRegions.stream()
                .peek(entry -> log.info("🔥 지역: {}, 조회수: {}", entry.getValue(), entry.getScore()))
                .map(entry -> HotRegionDTO.builder()
                        .name(entry.getValue().toString())
                        .searchCount(entry.getScore() != null ? entry.getScore().longValue() : 0)
                        .build())
                .collect(Collectors.toList());
    }
}