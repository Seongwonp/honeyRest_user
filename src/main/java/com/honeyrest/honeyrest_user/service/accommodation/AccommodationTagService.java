package com.honeyrest.honeyrest_user.service.accommodation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationTagDTO;
import com.honeyrest.honeyrest_user.entity.AccommodationTag;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class AccommodationTagService {

    private final AccommodationTagRepository tagRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String TAG_CACHE_KEY = "accommodation:tags:all";

    public List<AccommodationTagDTO> getAllTags() {
        Object raw = redisTemplate.opsForValue().get(TAG_CACHE_KEY);
        List<AccommodationTagDTO> cached = raw != null
                ? objectMapper.convertValue(raw, new TypeReference<List<AccommodationTagDTO>>() {})
                : null;

        if (cached != null && !cached.isEmpty()) {
            log.info("🚀 Redis 태그 캐시 HIT");
            return cached;
        }

        List<AccommodationTagDTO> tags = tagRepository.findAll().stream()
                .map(tag -> AccommodationTagDTO.builder()
                        .tagId(tag.getTagId())
                        .name(tag.getName())
                        .category(tag.getCategory())
                        .iconName(tag.getIconName())
                        .build())
                .toList();

        redisTemplate.opsForValue().set(TAG_CACHE_KEY, tags, Duration.ofHours(12));
        log.info("📦 Redis 태그 캐시 저장");

        return tags;
    }
}