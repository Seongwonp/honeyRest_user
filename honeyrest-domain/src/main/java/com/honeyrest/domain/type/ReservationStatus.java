package com.honeyrest.domain.type;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 예약 상태 문자열 상수 (reservation.status). 사용자 API 와 호스트 앱이 공유하는 기준이다.
 * 취소 철자는 CANCELLED (CANCELED 아님).
 */
public final class ReservationStatus {

    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCEL_REQUEST = "CANCEL_REQUEST";
    public static final String COMPLETED = "COMPLETED";
    public static final String NO_SHOW = "NO_SHOW";
    public static final String CANCELLED = "CANCELLED";

    /** 객실 재고를 점유하는 상태. CANCEL_REQUEST 는 호스트 승인 전까지 점유한다. */
    public static final List<String> OCCUPYING = List.of(PENDING, CONFIRMED, CANCEL_REQUEST, COMPLETED, NO_SHOW);

    /** 호스트 화면/검증에서 허용하는 전체 상태 목록. */
    public static final List<String> ALL = List.of(PENDING, CONFIRMED, CANCEL_REQUEST, COMPLETED, NO_SHOW, CANCELLED);

    /**
     * 허용되는 상태 전이 표 (from → to 집합). 같은 상태로의 전이는 없다.
     * <ul>
     *   <li>PENDING → CONFIRMED, NO_SHOW, CANCELLED</li>
     *   <li>CONFIRMED → CANCEL_REQUEST, COMPLETED, NO_SHOW, CANCELLED</li>
     *   <li>CANCEL_REQUEST → CANCELLED(승인), CONFIRMED(거부)</li>
     *   <li>COMPLETED, NO_SHOW → CANCELLED (관리자 취소)</li>
     *   <li>CANCELLED → CONFIRMED (관리자 취소 복구, 호출자가 재고 검사)</li>
     * </ul>
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
