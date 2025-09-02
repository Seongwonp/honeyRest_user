package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.honeyrest_user.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

}
