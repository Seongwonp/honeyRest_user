package com.honeyrest.domain.type;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 예약 상태 문자열 상수 (reservation.status VARCHAR(20)).
 * <p>
 * 사용자 API 와 호스트 관리자 앱이 같은 테이블을 공유하므로 이 클래스가 두 앱의 단일 기준이다.
 * (통합 전에는 두 저장소에 사본이 있어 "양쪽을 함께 수정" 해야 했다.)
 * 철자 주의: 취소는 CANCELLED — 과거 일부 쿼리에 쓰인 CANCELED 는 잘못된 값이다.
 */
public final class ReservationStatus {

    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCEL_REQUEST = "CANCEL_REQUEST";
    public static final String COMPLETED = "COMPLETED";
    public static final String NO_SHOW = "NO_SHOW";
    public static final String CANCELLED = "CANCELLED";

    /**
     * 객실 재고를 점유하는 상태 목록.
     * CANCELLED(및 오타 변형 CANCELED), REFUNDED 는 재고를 점유하지 않는다.
     * CANCEL_REQUEST 는 호스트가 승인하기 전까지 객실을 계속 점유한다.
     */
    public static final List<String> OCCUPYING = List.of(PENDING, CONFIRMED, CANCEL_REQUEST, COMPLETED, NO_SHOW);

    /** 호스트 화면/검증에서 허용하는 전체 상태 목록. */
    public static final List<String> ALL = List.of(PENDING, CONFIRMED, CANCEL_REQUEST, COMPLETED, NO_SHOW, CANCELLED);

    /**
     * 허용되는 상태 전이 표 (from → 허용되는 to 집합). 두 앱의 현재 사용처에서 도출했다.
     * <ul>
     *   <li>PENDING → CONFIRMED (확정), NO_SHOW (호스트 노쇼 처리), CANCELLED (취소)</li>
     *   <li>CONFIRMED → CANCEL_REQUEST (사용자 취소 요청), COMPLETED (호스트 체크아웃 완료·e2e 이용 완료),
     *       NO_SHOW (호스트 노쇼 처리), CANCELLED (관리자 취소)</li>
     *   <li>CANCEL_REQUEST → CANCELLED (호스트 취소 승인), CONFIRMED (호스트 취소 거부)</li>
     *   <li>COMPLETED, NO_SHOW → CANCELLED (관리자 취소: 기존 {@code Reservation.cancel} 이 취소 외 모든 상태에서 허용)</li>
     *   <li>CANCELLED → CONFIRMED (호스트 관리자 화면의 취소 복구. 점유 상태로 돌아가므로 호출자가 재고를 검사한다)</li>
     * </ul>
     * 같은 상태로의 전이(X → X)는 표에 없다. {@code Reservation.changeStatus} 는 이를 변경 없음(no-op)으로 처리한다.
     */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            PENDING, Set.of(CONFIRMED, NO_SHOW, CANCELLED),
            CONFIRMED, Set.of(CANCEL_REQUEST, COMPLETED, NO_SHOW, CANCELLED),
            CANCEL_REQUEST, Set.of(CANCELLED, CONFIRMED),
            COMPLETED, Set.of(CANCELLED),
            NO_SHOW, Set.of(CANCELLED),
            CANCELLED, Set.of(CONFIRMED)
    );

    private ReservationStatus() {
    }

    /**
     * from → to 전이가 허용되는지 (대소문자·앞뒤 공백 무시).
     * null, 모르는 상태, 같은 상태로의 전이는 false.
     */
    public static boolean canTransition(String from, String to) {
        if (from == null || to == null) return false;
        Set<String> allowed = TRANSITIONS.get(from.trim().toUpperCase(Locale.ROOT));
        return allowed != null && allowed.contains(to.trim().toUpperCase(Locale.ROOT));
    }

    /** from 에서 전이할 수 있는 상태 집합 (모르는 상태면 빈 집합). */
    public static Set<String> allowedTransitions(String from) {
        if (from == null) return Set.of();
        return TRANSITIONS.getOrDefault(from.trim().toUpperCase(Locale.ROOT), Set.of());
    }

    /** 재고를 점유하는 상태인지 여부 (대소문자 무시). */
    public static boolean isOccupying(String status) {
        return status != null && OCCUPYING.contains(status.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * 입력 상태를 대문자로 정규화하고 허용 목록에 없으면 IllegalArgumentException.
     * null/빈 값이면 defaultStatus 를 돌려준다.
     */
    public static String normalize(String status, String defaultStatus) {
        if (status == null || status.isBlank()) return defaultStatus;
        String s = status.trim().toUpperCase(Locale.ROOT);
        if (!ALL.contains(s)) {
            throw new IllegalArgumentException("유효하지 않은 예약 상태: " + status);
        }
        return s;
    }
}
