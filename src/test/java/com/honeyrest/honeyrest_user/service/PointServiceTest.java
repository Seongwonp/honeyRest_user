package com.honeyrest.honeyrest_user.service;

import com.honeyrest.domain.entity.PointHistory;
import com.honeyrest.domain.entity.User;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 잔액 변경과 이력 기록이 항상 함께 일어나는지(또는 함께 일어나지 않는지) 확인한다. */
@ExtendWith(MockitoExtension.class)
@DisplayName("PointService: 잔액 변경 + 이력 기록")
class PointServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PointHistoryRepository pointHistoryRepository;

    @InjectMocks private PointService pointService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1L).point(5000).build();
    }

    private PointHistory savedHistory() {
        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("적립: 잔액이 늘고 SAVE 이력(변동 후 잔액, 1년 만료)이 남는다")
    void earn() {
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        pointService.earn(user, 1000, "리뷰 작성 적립", 10L);

        assertThat(user.getPoint()).isEqualTo(6000);
        verify(userRepository).save(user);
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
    @DisplayName("사용: 잔액이 줄고 음수 금액의 USE 이력이 남는다")
    void use() {
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        pointService.use(user, 3000, "예약 시 포인트 사용", 20L);

        assertThat(user.getPoint()).isEqualTo(2000);
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
        assertThat(user.getPoint()).isZero();
    }

    @Test
    @DisplayName("잔액 부족: 예외이며 잔액·이력 모두 바뀌지 않는다")
    void useInsufficient() {
        assertThatThrownBy(() -> pointService.use(user, 5001, "예약 시 포인트 사용", 20L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("포인트가 부족");

        assertThat(user.getPoint()).isEqualTo(5000);
        verifyNoInteractions(userRepository, pointHistoryRepository);
    }

    @Test
    @DisplayName("환불: 잔액이 늘고 REFUND 이력이 남는다")
    void refund() {
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        pointService.refund(user, 500, "예약 취소 포인트 환불", 20L);

        assertThat(user.getPoint()).isEqualTo(5500);
        PointHistory h = savedHistory();
        assertThat(h.getAmount()).isEqualTo(500);
        assertThat(h.getType()).isEqualTo(PointService.TYPE_REFUND);
        assertThat(h.getBalance()).isEqualTo(5500);
    }

    @Test
    @DisplayName("0 이하 변동량과 사용자 없음은 거부한다")
    void rejectsNonPositiveAmountAndMissingUser() {
        assertThatThrownBy(() -> pointService.earn(user, 0, "x", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pointService.use(user, -1, "x", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pointService.refund(user, -100, "x", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pointService.earn(null, 100, "x", null)).isInstanceOf(IllegalArgumentException.class);

        assertThat(user.getPoint()).isEqualTo(5000);
        verifyNoInteractions(userRepository, pointHistoryRepository);
    }
}
