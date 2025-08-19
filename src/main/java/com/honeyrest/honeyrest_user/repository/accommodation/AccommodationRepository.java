package com.honeyrest.honeyrest_user.repository.accommodation;

import com.honeyrest.honeyrest_user.entity.Accommodation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long>, AccommodationSearch {

    Page<Accommodation> findByStatus(String status, Pageable pageable);

    Page<Accommodation> findByCategory_NameAndStatus(String categoryName, String status, Pageable pageable);

    @Query("SELECT MAX(a.minPrice) FROM Accommodation a WHERE a.status = :status")
    BigDecimal findMaxMinPriceByStatus(@Param("status") String status);

}
