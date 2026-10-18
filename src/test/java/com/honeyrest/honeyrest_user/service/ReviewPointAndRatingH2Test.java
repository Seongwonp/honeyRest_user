package com.honeyrest.honeyrest_user.service;

import com.honeyrest.domain.entity.*;
import com.honeyrest.domain.type.ReservationStatus;
import com.honeyrest.honeyrest_user.dto.review.ReviewRequestDTO;
import com.honeyrest.honeyrest_user.repository.review.ReviewRepository;
import com.honeyrest.honeyrest_user.support.H2IntegrationTestSupport;
import com.honeyrest.honeyrest_user.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** 리뷰 포인트 예약당 1회 적립 + 마지막 리뷰 삭제 시 평점 초기화 (실제 서비스 흐름, H2). */
@DisplayName("리뷰: 삭제 후 재작성 시 포인트 재적립 없음 · 마지막 리뷰 삭제 시 평점 0 (H2)")
class ReviewPointAndRatingH2Test extends H2IntegrationTestSupport {

    @Autowired private ReviewService reviewService;
    @Autowired private ReviewRepository reviewRepository;

    private Long countReviewSaveHistories(Long userId, Long reservationId) {
        return transactionTemplate.execute(s -> entityManager.createQuery(
                        "select count(h) from PointHistory h where h.user.userId = :u and h.relatedId = :r and h.type = 'SAVE'", Long.class)
                .setParameter("u", userId)
                .setParameter("r", reservationId)
                .getSingleResult());
    }

    @Test
    @DisplayName("리뷰 작성 → 삭제 → 재작성: 포인트는 처음 1회(1,000)만 적립되고, 삭제 시 회수하지 않는다")
    void rewriteDoesNotEarnAgain() {
        User user = fixtures.user(0);
        Accommodation acc = fixtures.accommodation(TestFixtures.unique("리뷰시"), "ACTIVE");
        Room room = fixtures.room(acc.getAccommodationId(), 1, 2, "ACTIVE");
        Reservation reservation = fixtures.reservation(user.getUserId(), room.getRoomId(), ReservationStatus.COMPLETED,
                LocalDate.now().minusDays(3), LocalDate.now().minusDays(1));
        ReviewRequestDTO request = ReviewRequestDTO.builder()
                .reservationId(reservation.getReservationId())
                .rating(new BigDecimal("4.0"))
                .content("정말 좋은 숙소였습니다. 또 오고 싶어요.")
                .build();

        reviewService.createReview(user.getUserId(), request);
        assertThat(fixtures.find(User.class, user.getUserId()).getPoint()).isEqualTo(1000);

        Long reviewId = reviewRepository.findByUser_UserId(user.getUserId(), org.springframework.data.domain.Pageable.unpaged())
                .getContent().get(0).getReviewId();
        reviewService.deleteReview(user.getUserId(), reviewId);
        assertThat(fixtures.find(User.class, user.getUserId()).getPoint()).as("삭제 시 회수하지 않음").isEqualTo(1000);

        reviewService.createReview(user.getUserId(), request);

        assertThat(fixtures.find(User.class, user.getUserId()).getPoint()).as("재작성 적립 0").isEqualTo(1000);
        assertThat(countReviewSaveHistories(user.getUserId(), reservation.getReservationId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("마지막 리뷰를 삭제하면 숙소 평점이 0.0 으로 돌아간다")
    void deletingLastReviewResetsRating() {
        User user = fixtures.user(0);
        Accommodation acc = fixtures.accommodation(TestFixtures.unique("평점시"), "ACTIVE");
        Room room = fixtures.room(acc.getAccommodationId(), 1, 2, "ACTIVE");
        Reservation reservation = fixtures.reservation(user.getUserId(), room.getRoomId(), ReservationStatus.COMPLETED,
                LocalDate.now().minusDays(3), LocalDate.now().minusDays(1));

        reviewService.createReview(user.getUserId(), ReviewRequestDTO.builder()
                .reservationId(reservation.getReservationId())
                .rating(new BigDecimal("5.0"))
                .content("최고의 숙소였습니다. 강력 추천합니다.")
                .build());
        assertThat(fixtures.find(Accommodation.class, acc.getAccommodationId()).getRating()).isEqualByComparingTo("5.0");

        Long reviewId = reviewRepository.findByUser_UserId(user.getUserId(), org.springframework.data.domain.Pageable.unpaged())
                .getContent().get(0).getReviewId();
        reviewService.deleteReview(user.getUserId(), reviewId);

        assertThat(fixtures.find(Accommodation.class, acc.getAccommodationId()).getRating()).isEqualByComparingTo("0.0");
    }
}
