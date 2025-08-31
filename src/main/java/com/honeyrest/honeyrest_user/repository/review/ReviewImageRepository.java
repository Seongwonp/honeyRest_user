package com.honeyrest.honeyrest_user.repository.review;

import com.honeyrest.honeyrest_user.entity.Review;
import com.honeyrest.honeyrest_user.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {
    List<ReviewImage> findByReview_ReviewIdIn(List<Long> reviewIds);

    void deleteByReview(Review review);
}
