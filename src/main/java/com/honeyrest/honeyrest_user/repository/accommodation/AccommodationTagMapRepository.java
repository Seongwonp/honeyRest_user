package com.honeyrest.honeyrest_user.repository.accommodation;

import com.honeyrest.honeyrest_user.entity.AccommodationTagMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccommodationTagMapRepository extends JpaRepository<AccommodationTagMap, Long> {

    // 숙소 ID로 태그 매핑 조회
    List<AccommodationTagMap> findByAccommodation_AccommodationId(Long accommodationId);

    // 숙소 ID로 태그 매핑 삭제
    void deleteByAccommodation_AccommodationId(Long accommodationId);
}