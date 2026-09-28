package com.honeyrest.honeyrest_user.entity;

import java.util.List;

/**
 * 예약 상태 문자열 상수.
 * <p>
 * reservation.status 는 VARCHAR 이며 호스트/관리자 저장소(honeyRest_host)와 같은 테이블을 공유한다.
 * 상태 이름을 바꾸거나 새 상태를 추가할 때는 양쪽 저장소를 함께 수정해야 한다.
 */
public final class ReservationStatus {

    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCEL_REQUEST = "CANCEL_REQUEST";
    public static final String COMPLETED = "COMPLETED";
    public static final String NO_SHOW = "NO_SHOW";
    public static final String CANCELLED = "CANCELLED";

    /**
     * 객실 재고를 점유하는 상태 목록 (호스트 쪽 validStatuses 와 동일).
     * CANCELLED(및 오타 변형 CANCELED), REFUNDED 는 재고를 점유하지 않는다.
     * CANCEL_REQUEST 는 호스트가 승인하기 전까지 객실을 계속 점유한다.
     */
    public static final List<String> OCCUPYING = List.of(PENDING, CONFIRMED, CANCEL_REQUEST, COMPLETED, NO_SHOW);

    private ReservationStatus() {
    }
}
