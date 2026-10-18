package com.honeyrest.honeyrest_user.service;

import com.honeyrest.domain.entity.PointHistory;
import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.repository.PointHistoryRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 포인트 잔액 변경과 이력(point_history) 기록을 한 트랜잭션에서 함께 처리한다.
 * <p>
 * 과거에는 호출자가 {@code UserService.usePoint/addPoint} 로 잔액을 바꾸고 {@code PointHistoryService} 로 이력을
 * 따로 남겨, 한쪽만 호출되거나 순서가 바뀌면 잔액과 이력(balance)이 어긋날 수 있었다(SRP).
 * 이제 잔액을 바꾸는 모든 경로는 이 서비스의 {@link #earn}/{@link #use}/{@link #refund} 만 쓴다.
 * <p>
 * 호출자의 트랜잭션에 참여하므로(기본 전파 REQUIRED) 예약 저장 등이 롤백되면 잔액·이력도 함께 롤백된다.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class PointService {

    /** 이력 type 값 (point_history.type). */
    public static final String TYPE_SAVE = "SAVE";
    public static final String TYPE_USE = "USE";
    public static final String TYPE_REFUND = "REFUND";

    /** 적립 포인트 유효 기간. */
    static final int EARN_VALID_YEARS = 1;

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    /** 포인트 적립 (유효기간 1년). */
    @Transactional
    public PointHistory earn(User user, int amount, String reason, Long relatedId) {
        requirePositive(amount);
        return apply(user, amount, TYPE_SAVE, reason, relatedId, LocalDateTime.now().plusYears(EARN_VALID_YEARS));
    }

    /** 포인트 사용. 잔액보다 많이 쓰면 IllegalArgumentException 이며 잔액·이력 모두 바뀌지 않는다. */
    @Transactional
    public PointHistory use(User user, int amount, String reason, Long relatedId) {
        requirePositive(amount);
        return apply(user, -amount, TYPE_USE, reason, relatedId, null);
    }

    /** 사용했던 포인트 환불(복원). */
    @Transactional
    public PointHistory refund(User user, int amount, String reason, Long relatedId) {
        requirePositive(amount);
        return apply(user, amount, TYPE_REFUND, reason, relatedId, null);
    }

    private PointHistory apply(User user, int delta, String type, String reason, Long relatedId, LocalDateTime expiresAt) {
        if (user == null) {
            throw new IllegalArgumentException("포인트 대상 사용자가 없습니다.");
        }
        int current = user.getPoint();
        int updated = current + delta;
        if (updated < 0) {
            throw new IllegalArgumentException("사용 가능한 포인트가 부족합니다");
        }

        user.updatePoint(updated);
        userRepository.save(user);

        PointHistory history = pointHistoryRepository.save(PointHistory.builder()
                .user(user)
                .amount(delta)
                .type(type)
                .reason(reason)
                .relatedId(relatedId)
                .balance(updated) // 변동 후 잔액
                .expiresAt(expiresAt)
                .build());

        log.info("🪙 포인트 {}: userId={}, amount={}, balance={}, relatedId={}",
                type, user.getUserId(), delta, updated, relatedId);
        return history;
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("포인트 변동량은 0보다 커야 합니다.");
        }
    }
}
