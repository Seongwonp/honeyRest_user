package com.honeyrest.honeyrest_user.service.accommodation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationTagMapDTO;
import com.honeyrest.honeyrest_user.entity.Accommodation;
import com.honeyrest.honeyrest_user.entity.AccommodationTag;
import com.honeyrest.honeyrest_user.entity.AccommodationTagMap;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationRepository;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationTagMapRepository;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class AccommodationTagMapService {

    private final AccommodationTagMapRepository tagMapRepository;
    private final AccommodationRepository accommodationRepository;
    private final AccommodationTagRepository tagRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public List<AccommodationTagMapDTO> getTagsByAccommodation(Long accommodationId) {
        String key = "accommodation:tags:" + accommodationId;

        Object raw = redisTemplate.opsForValue().get(key);
        List<AccommodationTagMapDTO> cached = raw != null
                ? objectMapper.convertValue(raw, new TypeReference<List<AccommodationTagMapDTO>>() {})
                : null;

        if (cached != null && !cached.isEmpty()) {
            log.info("🚀 Redis 캐시 HIT: {}", key);
            return cached;
        }

        List<AccommodationTagMapDTO> tags = tagMapRepository.findByAccommodation_AccommodationId(accommodationId)
                .stream()
                .map(map -> AccommodationTagMapDTO.builder()
                        .mapId(map.getMapId())
                        .accommodationId(map.getAccommodation().getAccommodationId())
                        .tagId(map.getTag().getTagId())
                        .tagName(map.getTag().getName())
                        .tagCategory(map.getTag().getCategory())
                        .iconName(map.getTag().getIconName())
                        .build())
                .toList();

        redisTemplate.opsForValue().set(key, tags, Duration.ofHours(6));
        log.info("📦 Redis 캐시 저장: {}", key);

        return tags;
    }

    public void addTagToAccommodation(Long accommodationId, Long tagId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId).orElseThrow();
        AccommodationTag tag = tagRepository.findById(tagId).orElseThrow();

        AccommodationTagMap map = AccommodationTagMap.builder()
                .accommodation(accommodation)
                .tag(tag)
                .build();

        tagMapRepository.save(map);
    }

    public void removeTagsFromAccommodation(Long accommodationId) {
        tagMapRepository.deleteByAccommodation_AccommodationId(accommodationId);
    }
}