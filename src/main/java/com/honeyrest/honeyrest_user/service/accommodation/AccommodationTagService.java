package com.honeyrest.honeyrest_user.service.accommodation;

import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationTagDTO;
import com.honeyrest.honeyrest_user.entity.AccommodationTag;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class AccommodationTagService {

    private final AccommodationTagRepository tagRepository;

    @Cacheable("accommodationTags")
    public List<AccommodationTagDTO> getAllTags() {
        List<AccommodationTag> tags = tagRepository.findAll();

        return tags.stream()
                .map(tag -> AccommodationTagDTO.builder()
                        .tagId(tag.getTagId())
                        .name(tag.getName())
                        .category(tag.getCategory())
                        .iconName(tag.getIconName())
                        .build())
                .toList();
    }
}