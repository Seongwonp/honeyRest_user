package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.honeyrest_user.entity.Banner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface BannerRepository extends JpaRepository<Banner, Integer> {
    List<Banner> findByIsActiveTrueAndStartDateBeforeAndEndDateAfterOrderBySortOrderAsc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT b FROM Banner b WHERE b.isActive = true AND b.startDate <= :now AND b.endDate >= :now AND b.title NOT LIKE '%PETER%' ORDER BY function('RAND')")
    List<Banner> findRandomBanner(@Param("now") LocalDateTime now, Pageable pageable);

    
}
