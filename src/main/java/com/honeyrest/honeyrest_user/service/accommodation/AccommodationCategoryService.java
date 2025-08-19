package com.honeyrest.honeyrest_user.service.accommodation;

import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationCategoryDTO;
import com.honeyrest.honeyrest_user.entity.AccommodationCategory;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class AccommodationCategoryService {

    private final AccommodationCategoryRepository categoryRepository;


    public List<AccommodationCategoryDTO> getAllCategories() {
        List<AccommodationCategory> categories = categoryRepository.findAll();

        return categories.stream()
                .map(cat -> AccommodationCategoryDTO.builder()
                        .categoryId(cat.getCategoryId())
                        .name(cat.getName())
                        .iconUrl(cat.getIconUrl())
                        .sortOrder(cat.getSortOrder())
                        .build())
                .toList();
    }
}