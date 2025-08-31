package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.dto.review.MyReviewDTO;
import com.honeyrest.honeyrest_user.dto.review.ReviewDTO;
import com.honeyrest.honeyrest_user.dto.review.ReviewRequestDTO;
import com.honeyrest.honeyrest_user.entity.Reservation;
import com.honeyrest.honeyrest_user.entity.Review;
import com.honeyrest.honeyrest_user.entity.ReviewImage;
import com.honeyrest.honeyrest_user.repository.review.ReviewImageRepository;
import com.honeyrest.honeyrest_user.repository.review.ReviewRedisLikeRepository;
import com.honeyrest.honeyrest_user.repository.review.ReviewRepository;
import com.honeyrest.honeyrest_user.repository.reservation.ReservationRepository;
import com.honeyrest.honeyrest_user.service.accommodation.AccommodationService;
import com.honeyrest.honeyrest_user.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository imageRepository;
    private final ReviewRedisLikeRepository reviewRedisLikeRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final AccommodationService accommodationService;
    private final FileUploadUtil  fileUploadUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public void createReview(ReviewRequestDTO request) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다"));

        if (reviewRepository.existsByReservation(reservation)) {
            throw new IllegalArgumentException("이미 리뷰가 작성된 예약입니다");
        }

        Review review = Review.builder()
                .reservation(reservation)
                .user(reservation.getUser())
                .accommodationId(reservation.getAccommodation().getAccommodationId())
                .roomId(reservation.getRoom().getRoomId())
                .rating(request.getRating())
                .cleanlinessRating(request.getCleanlinessRating())
                .facilitiesRating(request.getFacilitiesRating())
                .serviceRating(request.getServiceRating())
                .locationRating(request.getLocationRating())
                .content(request.getContent())
                .status("PUBLISHED")
                .build();

        reviewRepository.save(review);

        if (request.getImageUrls() != null) {
            for (String url : request.getImageUrls()) {
                ReviewImage image = ReviewImage.builder()
                        .review(review)
                        .imageUrl(url)
                        .build();
                reviewImageRepository.save(image);
            }
        }

        // 평점 업데이트
        accommodationService.updateRating(reservation.getAccommodation().getAccommodationId());
        // Redis 캐시 삭제
        Long accommodationId = reservation.getAccommodation().getAccommodationId();
        String reviewListKey = "reviewList:accommodation:" + accommodationId;
        String reviewCountKey = "reviewCount:accommodation:" + accommodationId;
        redisTemplate.delete(reviewListKey);
        redisTemplate.delete(reviewCountKey);
    }

    @Transactional
    public int toggleLike(Long reviewId, boolean isLiked) {
        if (isLiked) {
            reviewRedisLikeRepository.decreaseLikeCount(reviewId);
        } else {
            reviewRedisLikeRepository.increaseLikeCount(reviewId);
        }

        int redisCount = reviewRedisLikeRepository.getLikeCount(reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다"));
        review.setLikeCount(redisCount);
        reviewRepository.save(review);

        return redisCount;
    }


    public PageResponseDTO<MyReviewDTO> getUserReviews(Long userId, Pageable pageable) {
        // 1. 리뷰 페이징 조회
        Page<Review> page = reviewRepository.findByUser_UserId(userId, pageable);
        List<Long> reviewIds = page.stream().map(Review::getReviewId).toList();

        // 2. 이미지 조회
        List<ReviewImage> images = imageRepository.findByReview_ReviewIdIn(reviewIds);

        // 3. 이미지 매핑
        Map<Long, List<String>> imageMap = images.stream()
                .collect(Collectors.groupingBy(
                        img -> img.getReview().getReviewId(),
                        Collectors.mapping(ReviewImage::getImageUrl, Collectors.toList())
                ));

        // 4. DTO 변환
        List<MyReviewDTO> content = page.stream()
                .map(review -> MyReviewDTO.builder()
                        .reviewId(review.getReviewId())
                        .accommodationName(review.getReservation().getRoom().getAccommodation().getName())
                        .roomName(review.getReservation().getRoom().getName())
                        .content(review.getContent())
                        .reply(review.getReply())
                        .rating(review.getRating())
                        .createdAt(review.getCreatedAt())
                        .updatedAt(review.getUpdatedAt())
                        .imageUrls(imageMap.getOrDefault(review.getReviewId(), List.of()))
                        .build())
                .toList();

        // 5. 페이징 응답 구성
        return PageResponseDTO.<MyReviewDTO>builder()
                .content(content)
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }


    @Transactional
    public void updateReviewWithImages(Long userId, Long reviewId, ReviewRequestDTO dto, List<MultipartFile> newImages) throws Exception {
        // 1. 리뷰 조회 및 권한 확인
        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다"));

        if (!existing.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 리뷰만 수정할 수 있습니다");
        }

        // 2. 리뷰 필드 수정
        Review review = Review.builder()
                .reviewId(existing.getReviewId())
                .reservation(existing.getReservation())
                .user(existing.getUser())
                .accommodationId(existing.getAccommodationId())
                .roomId(existing.getRoomId())
                .rating(dto.getRating())
                .cleanlinessRating(dto.getCleanlinessRating())
                .serviceRating(dto.getServiceRating())
                .facilitiesRating(dto.getFacilitiesRating())
                .locationRating(dto.getLocationRating())
                .content(dto.getContent())
                .status(existing.getStatus())
                .build();

        reviewRepository.save(review);

        // 3. 기존 이미지 삭제 후 재등록
        imageRepository.deleteByReview(review); // 기존 이미지 삭제

        if (dto.getImageUrls() != null) {
            for (String url : dto.getImageUrls()) {
                ReviewImage image = ReviewImage.builder()
                        .review(review)
                        .imageUrl(url)
                        .build();
                imageRepository.save(image);
            }
        }

        // 4. 새 이미지 업로드 후 저장
        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile file : newImages) {
                String uploadedUrl = fileUploadUtil.upload(file, "reviews");
                ReviewImage image = ReviewImage.builder()
                        .review(review)
                        .imageUrl(uploadedUrl)
                        .build();
                imageRepository.save(image);
            }
        }
        // 5. 평점 재계산
        accommodationService.updateRating(review.getAccommodationId());
        // Redis 캐시 삭제
        Long accommodationId = review.getAccommodationId();
        String reviewListKey = "reviewList:accommodation:" + accommodationId;
        String reviewCountKey = "reviewCount:accommodation:" + accommodationId;
        redisTemplate.delete(reviewListKey);
        redisTemplate.delete(reviewCountKey);
    }


    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다"));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 리뷰만 삭제할 수 있습니다");
        }

        imageRepository.deleteByReview(review); // 이미지 먼저 삭제
        reviewRepository.delete(review);       // 리뷰 삭제

        accommodationService.updateRating(review.getAccommodationId()); // 평점 재계산
        // Redis 캐시 삭제
        Long accommodationId = review.getAccommodationId();
        String reviewListKey = "reviewList:accommodation:" + accommodationId;
        String reviewCountKey = "reviewCount:accommodation:" + accommodationId;
        redisTemplate.delete(reviewListKey);
        redisTemplate.delete(reviewCountKey);
    }

}
