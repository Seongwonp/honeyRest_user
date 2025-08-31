package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.honeyrest_user.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Integer> {
    List<Region> findByLevelAndParentId(Integer level, Integer parentId);

    List<Region> findByIsPopularOrderByRegionIdAsc(Boolean isPopular);
}