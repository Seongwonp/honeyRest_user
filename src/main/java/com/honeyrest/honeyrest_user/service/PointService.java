package com.honeyrest.honeyrest_user.service;

import com.honeyrest.domain.entity.PointHistory;
import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.exception.NotFoundException;
import com.honeyrest.honeyrest_user.repository.PointHistoryRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 포인트 잔액 변경과 이력(point_history) 기록을 한 트랜잭션에서 함께 처리한다.
 * 잔액을 바꾸는 경로는 모두 이 서비스를 거친다. 잔액은 조건부/증분 UPDATE 로 원자적으로 바꾸고,
 * 이력에는 변동 후 DB 잔액을 남긴다. 호출자 트랜잭션에 참여하므로 함께 롤백된다.
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

    /**
     * 1회성 적립: 같은 사용자·사유·관련 ID 로 이미 적립 이력이 있으면 적립하지 않는다 (리뷰 적립용).
     * 동시 호출 직렬화는 호출자 책임이다 (리뷰는 예약 행 FOR UPDATE 락 안에서 호출한다).
     *
     * @return 적립 이력, 이미 적립된 적이 있으면 null
     */
    @Transactional
    public PointHistory earnOnce(User user, int amount, String reason, Long relatedId) {
        requireUser(user);
        if (relatedId != null && pointHistoryRepository.existsByUser_UserIdAndTypeAndReasonAndRelatedId(
                user.getUserId(), TYPE_SAVE, reason, relatedId)) {
            log.info("🪙 이미 적립된 1회성 포인트라 건너뜀: userId={}, reason={}, relatedId={}",
                    user.getUserId(), reason, relatedId);
            return null;
        }
        return earn(user, amount, reason, relatedId);
    }

    /**
     * 포인트 사용. 잔액 차감은 조건부 UPDATE({@code point >= amount})로 원자적으로 한다.
     * 잔액이 부족하면(동시 사용으로 먼저 소진된 경우 포함) 409 ApiException 이며 잔액·이력 모두 바뀌지 않는다.
     */
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
        requireUser(user);
        Long userId = user.getUserId();

        // read-modify-write 로 덮어쓰면 동시 요청이 같은 포인트를 두 번 쓸 수 있어 DB 에서 원자적으로 바꾼다.
        int changed = delta < 0
                ? userRepository.deductPointIfEnough(userId, -delta)
                : userRepository.addPoint(userId, delta);
        if (changed == 0) {
            if (delta < 0) {
                throw new ApiException("사용 가능한 포인트가 부족합니다", HttpStatus.CONFLICT);
            }
            throw new NotFoundException("포인트 대상 사용자를 찾을 수 없습니다.");
        }

        // 방금 UPDATE 한 행은 이 트랜잭션이 잠그고 있으므로 다시 읽은 값이 곧 변동 후 잔액이다.
        int updated = userRepository.findPointByUserId(userId)
                .orElseThrow(() -> new NotFoundException("포인트 대상 사용자를 찾을 수 없습니다."));
        // 같은 트랜잭션에서 user.getPoint() 를 읽는 코드를 위해 엔티티도 맞춘다.
        user.updatePoint(updated);

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
                type, userId, delta, updated, relatedId);
        return history;
    }

    private static void requireUser(User user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("포인트 대상 사용자가 없습니다.");
        }
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("포인트 변동량은 0보다 커야 합니다.");
        }
    }
}
