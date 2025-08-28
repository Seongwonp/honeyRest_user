package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.honeyrest_user.entity.WishList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishListRepository extends JpaRepository<WishList, Long> {
    Optional<WishList> findByUser_UserIdAndAccommodation_AccommodationId(Long userId, Long accommodationId);
}
