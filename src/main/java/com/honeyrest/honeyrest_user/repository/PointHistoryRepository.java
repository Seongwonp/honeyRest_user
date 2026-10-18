package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.domain.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 같은 사용자·유형·사유·관련 ID 의 이력이 있는지 (1회성 적립 중복 방지). */
    boolean existsByUser_UserIdAndTypeAndReasonAndRelatedId(Long userId, String type, String reason, Long relatedId);

}
