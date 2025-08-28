package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.honeyrest_user.entity.CancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {
    List<CancellationPolicy> findByAccommodation_AccommodationId(Long accommodationId);
}
