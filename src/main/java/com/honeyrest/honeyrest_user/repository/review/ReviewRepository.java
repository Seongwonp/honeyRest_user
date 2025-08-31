package com.honeyrest.honeyrest_user.repository.review;

import com.honeyrest.honeyrest_user.entity.Reservation;
import com.honeyrest.honeyrest_user.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByReservation(Reservation reservation);

    @Query("SELECT r.rating FROM Review r WHERE r.accommodationId = :accommodationId AND r.status = 'PUBLISHED'")
    List<BigDecimal> findRatingsByAccommodationId(@Param("accommodationId") Long accommodationId);

    Page<Review> findByUser_UserId(Long userId, Pageable pageable);
}
