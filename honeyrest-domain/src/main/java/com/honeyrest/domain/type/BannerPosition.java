package com.honeyrest.domain.type;

/** 배너 노출 위치 (banner.position, @Enumerated(STRING)). Banner 엔티티가 참조하므로 공유 모듈에 둔다. */
public enum BannerPosition {
    MAIN_TOP,
    MAIN_MIDDLE,
    CATEGORY_TOP
}
