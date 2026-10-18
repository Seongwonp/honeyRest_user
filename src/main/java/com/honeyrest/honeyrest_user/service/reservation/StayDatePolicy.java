package com.honeyrest.honeyrest_user.service.reservation;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 숙박 날짜 규칙 (서버 검증의 단일 출처). 예약 폼 조회와 결제 승인(예약 생성) 전에 확인한다.
 * <ul>
 *   <li>체크인 ≥ 오늘 (Asia/Seoul 기준 — 서버 JVM 시간대와 무관)</li>
 *   <li>체크아웃 &gt; 체크인</li>
 *   <li>최대 {@value #MAX_NIGHTS}박</li>
 * </ul>
 * 위반 시 {@link IllegalArgumentException}(→ 400) 을 던진다. 과거에는 지난 날짜도 그대로 예약·결제됐다.
 */
public final class StayDatePolicy {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    public static final int MAX_NIGHTS = 30;

    private StayDatePolicy() {
    }

    /** 오늘 (Asia/Seoul). */
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static void validate(LocalDate checkIn, LocalDate checkOut) {
        validate(checkIn, checkOut, Clock.system(ZONE));
    }

    static void validate(LocalDate checkIn, LocalDate checkOut, Clock clock) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("체크인/체크아웃 날짜를 입력해 주세요.");
        }
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        if (checkIn.isBefore(today)) {
            throw new IllegalArgumentException("체크인 날짜는 오늘(" + today + ") 이후여야 합니다.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("체크아웃 날짜는 체크인 날짜보다 뒤여야 합니다.");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_NIGHTS) {
            throw new IllegalArgumentException("최대 " + MAX_NIGHTS + "박까지 예약할 수 있습니다. (요청: " + nights + "박)");
        }
    }
}
