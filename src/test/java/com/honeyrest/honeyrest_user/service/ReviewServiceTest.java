package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.review.ReviewRequestDTO;
import com.honeyrest.honeyrest_user.entity.*;
import com.honeyrest.honeyrest_user.repository.review.ReviewImageRepository;
import com.honeyrest.honeyrest_user.repository.review.ReviewRedisLikeRepository;
import com.honeyrest.honeyrest_user.repository.review.ReviewRepository;
import com.honeyrest.honeyrest_user.repository.reservation.ReservationRepository;
import com.honeyrest.honeyrest_user.service.accommodation.AccommodationService;
import com.honeyrest.honeyrest_user.service.redis.RatingCacheService;
import com.honeyrest.honeyrest_user.service.redis.SearchCacheVersionService;
import com.honeyrest.honeyrest_user.storage.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ReviewService 테스트")
class ReviewServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ReviewImageRepository imageRepository;
    @Mock private ReviewRedisLikeRepository reviewRedisLikeRepository;
    @Mock private RatingCacheService ratingCacheService;
    @Mock private AccommodationService accommodationService;
    @Mock private PointHistoryService pointHistoryService;
    @Mock private UserService userService;
    @Mock private FileStorage fileStorage;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private SearchCacheVersionService searchCacheVersionService;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private Accommodation accommodation;
    private Room room;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = User.builder()
                .userId(1L)
                .name("홍길동")
                .email("hong@example.com")
                .point(0)
                .build();

        accommodation = Accommodation.builder()
                .accommodationId(1L)
                .name("테스트 숙소")
                .build();

        room = Room.builder()
                .roomId(1L)
                .name("디럭스")
                .accommodation(accommodation)
                .price(BigDecimal.valueOf(100000))
                .maxOccupancy(2)
                .standardOccupancy(2)
                .build();

        reservation = Reservation.builder()
                .reservationId(1L)
                .user(user)
                .room(room)
                .accommodation(accommodation)
                .reservationNumber("RES-001")
                .status("COMPLETED")
                .price(BigDecimal.valueOf(100000))
                .build();
    }

    // ── createReview ────────────────────────────────────────

    @Test
    @DisplayName("리뷰 작성 성공")
    void createReview_success() {
        ReviewRequestDTO request = ReviewRequestDTO.builder()
                .reservationId(1L)
                .rating(BigDecimal.valueOf(4.5))
                .content("깨끗하고 좋았습니다. 다음에 또 오고 싶어요.")
                .build();

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));
        when(reviewRepository.existsByReservation(reservation)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(Review.builder().reviewId(1L).build());
        doNothing().when(ratingCacheService).evictAllAccommodationCache(any());

        assertThatCode(() -> reviewService.createReview(1L, request))
                .doesNotThrowAnyException();

        verify(reviewRepository).save(any(Review.class));
        verify(accommodationService).updateRating(1L);
    }

    @Test
    @DisplayName("예약 없는 리뷰 작성 시 예외 발생")
    void createReview_reservationNotFound() {
        ReviewRequestDTO request = ReviewRequestDTO.builder()
                .reservationId(999L)
                .rating(BigDecimal.valueOf(5))
                .content("테스트 리뷰입니다. 잘 지냈어요.")
                .build();

        when(reservationRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("예약 정보를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("중복 리뷰 작성 시 예외 발생")
    void createReview_duplicateReview() {
        ReviewRequestDTO request = ReviewRequestDTO.builder()
                .reservationId(1L)
                .rating(BigDecimal.valueOf(3))
                .content("두 번째 리뷰 시도")
                .build();

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));
        when(reviewRepository.existsByReservation(reservation)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 리뷰가 작성된 예약");
    }

    @Test
    @DisplayName("타인의 예약으로 리뷰 작성 시 예외 발생")
    void createReview_notOwner() {
        ReviewRequestDTO request = ReviewRequestDTO.builder()
                .reservationId(1L)
                .rating(BigDecimal.valueOf(4))
                .content("남의 예약에 리뷰를 써보려는 시도입니다.")
                .build();

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reviewService.createReview(99L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 예약");
    }

    @Test
    @DisplayName("이용 완료되지 않은 예약으로 리뷰 작성 시 예외 발생")
    void createReview_notCompleted() {
        Reservation confirmedOnly = Reservation.builder()
                .reservationId(1L)
                .user(user)
                .room(room)
                .accommodation(accommodation)
                .reservationNumber("RES-001")
                .status("CONFIRMED")
                .price(BigDecimal.valueOf(100000))
                .build();

        ReviewRequestDTO request = ReviewRequestDTO.builder()
                .reservationId(1L)
                .rating(BigDecimal.valueOf(4))
                .content("체크인 전인데 리뷰를 써보려는 시도입니다.")
                .build();

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(confirmedOnly));

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이용이 완료된 예약");
    }

    // ── toggleLike ─────────────────────────────────────────

    @Test
    @DisplayName("좋아요 추가 성공: 처음 누른 사용자면 카운터가 증가한다")
    void toggleLike_increase() {
        Review review = Review.builder()
                .reviewId(1L)
                .likeCount(5)
                .status("PUBLISHED")
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRedisLikeRepository.addLiker(1L, 7L)).thenReturn(true);
        when(reviewRedisLikeRepository.getLikeCount(1L)).thenReturn(6);

        int count = reviewService.toggleLike(7L, 1L, false);

        assertThat(count).isEqualTo(6);
        assertThat(review.getLikeCount()).isEqualTo(6);
        verify(reviewRedisLikeRepository).initLikeCountIfAbsent(1L, 5);
        verify(reviewRedisLikeRepository).increaseLikeCount(1L);
    }

    @Test
    @DisplayName("좋아요 멱등: 이미 좋아요한 사용자가 다시 요청하면 카운터가 변하지 않는다")
    void toggleLike_idempotentPerUser() {
        Review review = Review.builder()
                .reviewId(1L)
                .likeCount(6)
                .status("PUBLISHED")
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRedisLikeRepository.addLiker(1L, 7L)).thenReturn(false);
        when(reviewRedisLikeRepository.getLikeCount(1L)).thenReturn(6);

        int count = reviewService.toggleLike(7L, 1L, false);

        assertThat(count).isEqualTo(6);
        verify(reviewRedisLikeRepository, never()).increaseLikeCount(any());
    }

    @Test
    @DisplayName("좋아요 취소 성공: 좋아요한 적 있는 사용자만 카운터를 감소시킨다")
    void toggleLike_decrease() {
        Review review = Review.builder()
                .reviewId(1L)
                .likeCount(5)
                .status("PUBLISHED")
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRedisLikeRepository.removeLiker(1L, 7L)).thenReturn(true);
        when(reviewRedisLikeRepository.getLikeCount(1L)).thenReturn(4);

        int count = reviewService.toggleLike(7L, 1L, true);

        assertThat(count).isEqualTo(4);
        verify(reviewRedisLikeRepository).decreaseLikeCount(1L);
    }

    @Test
    @DisplayName("좋아요한 적 없는 사용자의 취소 요청은 카운터를 감소시키지 않는다")
    void toggleLike_unlikeWithoutLikeIsNoop() {
        Review review = Review.builder()
                .reviewId(1L)
                .likeCount(5)
                .status("PUBLISHED")
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRedisLikeRepository.removeLiker(1L, 8L)).thenReturn(false);
        when(reviewRedisLikeRepository.getLikeCount(1L)).thenReturn(5);

        assertThat(reviewService.toggleLike(8L, 1L, true)).isEqualTo(5);
        verify(reviewRedisLikeRepository, never()).decreaseLikeCount(any());
    }

    // ── updateReviewWithImages ─────────────────────────────

    @Test
    @DisplayName("리뷰 수정은 평점·본문만 바꾸고 createdAt·likeCount·reply·status 를 보존한다")
    void updateReview_preservesCreatedAtAndLikeCount() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        Review existing = Review.builder()
                .reviewId(1L)
                .reservation(reservation)
                .user(user)
                .accommodationId(1L)
                .roomId(1L)
                .rating(BigDecimal.valueOf(3))
                .content("원래 내용입니다.")
                .reply("사장님 답글")
                .likeCount(12)
                .status("PUBLISHED")
                .build();
        ReflectionTestUtils.setField(existing, "createdAt", createdAt);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(existing));

        ReviewRequestDTO dto = ReviewRequestDTO.builder()
                .reservationId(1L)
                .rating(BigDecimal.valueOf(5))
                .content("수정한 내용입니다. 정말 좋았어요.")
                .imageUrls(List.of())
                .build();

        reviewService.updateReviewWithImages(1L, 1L, dto, null);

        assertThat(existing.getRating()).isEqualByComparingTo("5");
        assertThat(existing.getContent()).isEqualTo("수정한 내용입니다. 정말 좋았어요.");
        assertThat(existing.getCreatedAt()).isEqualTo(createdAt);
        assertThat(existing.getLikeCount()).isEqualTo(12);
        assertThat(existing.getReply()).isEqualTo("사장님 답글");
        assertThat(existing.getStatus()).isEqualTo("PUBLISHED");
        // 새 Review 객체를 save(merge) 하지 않는다.
        verify(reviewRepository, never()).save(any(Review.class));
        verify(searchCacheVersionService).bumpAfterCommit();
    }

    // ── deleteReview ────────────────────────────────────────

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReview_success() {
        Review review = Review.builder()
                .reviewId(1L)
                .user(user)
                .accommodationId(1L)
                .roomId(1L)
                .status("PUBLISHED")
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        doNothing().when(imageRepository).deleteByReview(review);
        doNothing().when(reviewRepository).delete(review);
        doNothing().when(ratingCacheService).evictAllAccommodationCache(any());

        assertThatCode(() -> reviewService.deleteReview(1L, 1L))
                .doesNotThrowAnyException();

        verify(imageRepository).deleteByReview(review);
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("타인의 리뷰 삭제 시 예외 발생")
    void deleteReview_notOwner() {
        User otherUser = User.builder().userId(99L).build();

        Review review = Review.builder()
                .reviewId(1L)
                .user(otherUser)
                .accommodationId(1L)
                .roomId(1L)
                .status("PUBLISHED")
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 리뷰만 삭제");
    }
}
