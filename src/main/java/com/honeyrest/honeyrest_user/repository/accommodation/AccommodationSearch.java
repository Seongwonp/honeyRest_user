package com.honeyrest.honeyrest_user.repository.accommodation;

import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSearchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AccommodationSearch {
    public Page<AccommodationSearchDTO> searchAvailable(
            String location,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            Long userId,
            String sort,
            List<String> selectedCategories,
            List<String> selectedTags,
            BigDecimal maxPrice,
            Pageable pageable
    );
}
