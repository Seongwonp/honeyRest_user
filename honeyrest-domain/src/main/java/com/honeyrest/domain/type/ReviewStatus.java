package com.honeyrest.domain.type;

import java.util.List;
import java.util.Locale;

/**
 * 리뷰 상태 문자열 상수 (review.status VARCHAR(20)).
 * <p>
 * 사용자 API 는 리뷰를 {@link #PUBLISHED} 로 저장하고 PUBLISHED 만 사용자 화면·평점 집계에 쓴다.
 * 호스트 관리자 앱은 PUBLISHED(노출) / HIDDEN(숨김) / DELETED(삭제) 를 쓴다.
 * 과거 호스트가 노출 상태로 저장하던 {@code VISIBLE} 은 Flyway V12 에서 PUBLISHED 로 정규화했고,
 * 요청 값으로 들어오면 {@link #normalize(String)} 가 PUBLISHED 로 바꾼다(저장 값으로는 쓰지 않는다).
 */
public final class ReviewStatus {

    /** 노출 (사용자 화면에 보임) */
    public static final String PUBLISHED = "PUBLISHED";
    /** 숨김 (관리자 숨김, 소프트 삭제) */
    public static final String HIDDEN = "HIDDEN";
    /** 삭제 (관리자 삭제 처리) */
    public static final String DELETED = "DELETED";

    /** 과거 호스트가 쓰던 노출 값. 읽기·요청 호환용이며 저장하지 않는다. */
    public static final String LEGACY_VISIBLE = "VISIBLE";

    /** 저장할 수 있는 상태 목록 */
    public static final List<String> ALL = List.of(PUBLISHED, HIDDEN, DELETED);

    private ReviewStatus() {
    }

    /**
     * 요청/화면의 상태 값을 저장용 값으로 정규화한다 (앞뒤 공백 제거, 대문자, VISIBLE → PUBLISHED).
     * null·빈 값·허용되지 않은 값이면 {@link IllegalArgumentException}.
     */
    public static String normalize(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("리뷰 상태가 비어 있습니다.");
        }
        String s = status.trim().toUpperCase(Locale.ROOT);
        if (LEGACY_VISIBLE.equals(s)) {
            return PUBLISHED;
        }
        if (!ALL.contains(s)) {
            throw new IllegalArgumentException("유효하지 않은 리뷰 상태: " + status);
        }
        return s;
    }

    /** 사용자 화면에 노출되는 상태인지 (대소문자·공백 무시, 과거 VISIBLE 포함) */
    public static boolean isPublished(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase(Locale.ROOT);
        return PUBLISHED.equals(s) || LEGACY_VISIBLE.equals(s);
    }
}
