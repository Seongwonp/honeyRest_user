package com.honeyrest.honeyrest_user.service.accommodation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationCategoryDTO;
import com.honeyrest.honeyrest_user.entity.AccommodationCategory;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class AccommodationCategoryService {

    private final AccommodationCategoryRepository categoryRepository;
    private final RedisTemplate<String, String> redisTemplate; // 수정: String 타입 사용
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CACHE_KEY = "category:accommodation";

    public List<AccommodationCategoryDTO> getAllCategories() {
        String cachedJson = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cachedJson != null) {
            try {
                List<AccommodationCategoryDTO> cachedCategories = objectMapper.readValue(cachedJson, new TypeReference<List<AccommodationCategoryDTO>>() {});
                log.info("Cache HIT for key {}", CACHE_KEY);
                return cachedCategories;
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize cached data for key {}: {}", CACHE_KEY, e.getMessage());
                // proceed to load from DB
            }
        }
        log.info("Cache MISS for key {}", CACHE_KEY);

        List<AccommodationCategory> categories = categoryRepository.findAll();

        List<AccommodationCategoryDTO> categoryDTOs = categories.stream()
                .map(cat -> AccommodationCategoryDTO.builder()
                        .categoryId(cat.getCategoryId())
                        .name(cat.getName())
                        .iconUrl(cat.getIconUrl())
                        .sortOrder(cat.getSortOrder())
                        .build())
                .toList();

        try {
            String jsonToCache = objectMapper.writeValueAsString(categoryDTOs);
            redisTemplate.opsForValue().set(CACHE_KEY, jsonToCache);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize data for caching for key {}: {}", CACHE_KEY, e.getMessage());
        }

        return categoryDTOs;
    }
}