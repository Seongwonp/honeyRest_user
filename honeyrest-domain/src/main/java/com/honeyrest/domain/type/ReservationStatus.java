package com.honeyrest.domain.type;

import java.util.List;
import java.util.Locale;

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

    private ReservationStatus() {
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
