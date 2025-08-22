package com.honeyrest.honeyrest_user.service.accommodation;

import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationTagMapDTO;
import com.honeyrest.honeyrest_user.entity.Accommodation;
import com.honeyrest.honeyrest_user.entity.AccommodationTag;
import com.honeyrest.honeyrest_user.entity.AccommodationTagMap;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationRepository;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationTagMapRepository;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class AccommodationTagMapService {

    private final AccommodationTagMapRepository tagMapRepository;
    private final AccommodationRepository accommodationRepository;
    private final AccommodationTagRepository tagRepository;

    public List<AccommodationTagMapDTO> getTagsByAccommodation(Long accommodationId) {
        return tagMapRepository.findByAccommodation_AccommodationId(accommodationId)
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