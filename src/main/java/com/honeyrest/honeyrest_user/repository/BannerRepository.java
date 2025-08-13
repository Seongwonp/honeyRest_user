package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.honeyrest_user.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;


public interface BannerRepository extends JpaRepository<Banner, Integer> {
    List<Banner> findByIsActiveTrueAndStartDateBeforeAndEndDateAfterOrderBySortOrderAsc(LocalDateTime start, LocalDateTime end);
}
