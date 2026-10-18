package com.honeyrest.honeyrest_user.service;

import com.honeyrest.domain.entity.PointHistory;
import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.repository.PointHistoryRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 잔액 변경(원자적 UPDATE)과 이력 기록이 항상 함께 일어나는지(또는 함께 일어나지 않는지) 확인한다.
 * UserRepository 의 조건부/증분 UPDATE 는 {@link #dbPoint} 로 흉내 낸다 (실제 DB 동시성은 H2/MySQL 통합 테스트에서 확인).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PointService: 원자적 잔액 변경 + 이력 기록")
class PointServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PointHistoryRepository pointHistoryRepository;

    @InjectMocks private PointService pointService;

    private User user;
    /** DB 의 user.point 값. */
    private final AtomicInteger dbPoint = new AtomicInteger();

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1L).point(5000).build();
        dbPoint.set(5000);

        when(userRepository.deductPointIfEnough(eq(1L), anyInt())).thenAnswer(inv -> {
            int amount = inv.getArgument(1);
            if (dbPoint.get() < amount) {
                return 0; // WHERE point >= :amount 불충족
            }
            dbPoint.addAndGet(-amount);
            return 1;
        });
        when(userRepository.addPoint(eq(1L), anyInt())).thenAnswer(inv -> {
            dbPoint.addAndGet(inv.<Integer>getArgument(1));
            return 1;
        });
        when(userRepository.findPointByUserId(1L)).thenAnswer(inv -> Optional.of(dbPoint.get()));
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PointHistory savedHistory() {
        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("적립: 증분 UPDATE 로 잔액이 늘고 SAVE 이력(변동 후 잔액, 1년 만료)이 남는다")
    void earn() {
        pointService.earn(user, 1000, "리뷰 작성 적립", 10L);

        verify(userRepository).addPoint(1L, 1000);
        assertThat(dbPoint.get()).isEqualTo(6000);
        assertThat(user.getPoint()).as("관리 엔티티도 DB 값과 맞춘다").isEqualTo(6000);
        PointHistory h = savedHistory();
        assertThat(h.getUser()).isSameAs(user);
        assertThat(h.getAmount()).isEqualTo(1000);
        assertThat(h.getType()).isEqualTo(PointService.TYPE_SAVE);
        assertThat(h.getReason()).isEqualTo("리뷰 작성 적립");
        assertThat(h.getRelatedId()).isEqualTo(10L);
        assertThat(h.getBalance()).isEqualTo(6000);
        assertThat(h.getExpiresAt()).isAfter(LocalDateTime.now().plusMonths(11));
    }

    @Test
    @DisplayName("사용: 조건부 UPDATE(point >= amount)로 차감하고 음수 금액의 USE 이력이 남는다")
    void use() {
        pointService.use(user, 3000, "예약 시 포인트 사용", 20L);

        verify(userRepository).deductPointIfEnough(1L, 3000);
        // 엔티티 값으로 계산한 잔액을 save 로 덮어쓰지 않는다 (read-modify-write 금지)
        verify(userRepository, never()).save(any());
        assertThat(dbPoint.get()).isEqualTo(2000);
        PointHistory h = savedHistory();
        assertThat(h.getAmount()).isEqualTo(-3000);
        assertThat(h.getType()).isEqualTo(PointService.TYPE_USE);
        assertThat(h.getBalance()).isEqualTo(2000);
        assertThat(h.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("잔액 전부 사용은 허용된다 (0 이 된다)")
    void useAll() {
        pointService.use(user, 5000, "예약 시 포인트 사용", 20L);
        assertThat(dbPoint.get()).isZero();
        assertThat(user.getPoint()).isZero();
    }

    @Test
    @DisplayName("잔액 부족(조건부 UPDATE 0행): 409 이며 이력이 남지 않는다")
    void useInsufficient() {
        assertThatThrownBy(() -> pointService.use(user, 5001, "예약 시 포인트 사용", 20L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("포인트가 부족")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(dbPoint.get()).isEqualTo(5000);
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("동시 사용: 엔티티는 둘 다 잔액 5,000 으로 보고 있어도 DB 조건부 UPDATE 로 두 번째 사용은 409")
    void secondUseWithStaleEntityIsRejected() {
        User staleCopy = User.builder().userId(1L).point(5000).build(); // 다른 요청이 같은 시점에 읽은 엔티티

        pointService.use(user, 4000, "예약 시 포인트 사용", 1L);

        assertThatThrownBy(() -> pointService.use(staleCopy, 4000, "예약 시 포인트 사용", 2L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("포인트가 부족");
        assertThat(dbPoint.get()).isEqualTo(1000);
    }

    @Test
    @DisplayName("환불: 잔액이 늘고 REFUND 이력이 남는다")
    void refund() {
        pointService.refund(user, 500, "예약 취소 포인트 환불", 20L);

        assertThat(dbPoint.get()).isEqualTo(5500);
        PointHistory h = savedHistory();
        assertThat(h.getAmount()).isEqualTo(500);
        assertThat(h.getType()).isEqualTo(PointService.TYPE_REFUND);
        assertThat(h.getBalance()).isEqualTo(5500);
    }

    @Test
    @DisplayName("1회성 적립: 같은 사유·관련 ID 의 SAVE 이력이 이미 있으면 적립하지 않는다")
    void earnOnceSkipsWhenAlreadyEarned() {
        when(pointHistoryRepository.existsByUser_UserIdAndTypeAndReasonAndRelatedId(
                1L, PointService.TYPE_SAVE, "리뷰 작성 적립", 10L)).thenReturn(true);

        PointHistory result = pointService.earnOnce(user, 1000, "리뷰 작성 적립", 10L);

        assertThat(result).isNull();
        assertThat(dbPoint.get()).isEqualTo(5000);
        verify(userRepository, never()).addPoint(anyLong(), anyInt());
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("1회성 적립: 이력이 없으면 적립한다")
    void earnOnceEarnsFirstTime() {
        when(pointHistoryRepository.existsByUser_UserIdAndTypeAndReasonAndRelatedId(
                anyLong(), anyString(), anyString(), anyLong())).thenReturn(false);

        PointHistory result = pointService.earnOnce(user, 1000, "리뷰 작성 적립", 10L);

        assertThat(result).isNotNull();
        assertThat(dbPoint.get()).isEqualTo(6000);
    }

    @Test
    @DisplayName("0 이하 변동량과 사용자 없음은 거부한다")
    void rejectsNonPositiveAmountAndMissingUser() {
        assertThatThrownBy(() -> pointService.earn(user, 0, "x", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pointService.use(user, -1, "x", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pointService.refund(user, -100, "x", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pointService.earn(null, 100, "x", null)).isInstanceOf(IllegalArgumentException.class);

        assertThat(dbPoint.get()).isEqualTo(5000);
        verify(userRepository, never()).addPoint(anyLong(), anyInt());
        verify(userRepository, never()).deductPointIfEnough(anyLong(), anyInt());
        verify(pointHistoryRepository, never()).save(any());
    }
}
