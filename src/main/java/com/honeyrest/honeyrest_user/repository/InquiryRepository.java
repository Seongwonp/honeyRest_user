package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.honeyrest_user.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    Page<Inquiry> findAllByUserUserId(Long userId, Pageable pageable);

    long countByUserUserId(Long userUserId);

    long countByUserUserIdAndIsRepliedTrue(Long userId);
}
