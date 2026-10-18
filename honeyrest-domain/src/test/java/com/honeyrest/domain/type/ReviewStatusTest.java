package com.honeyrest.domain.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 공유 ReviewStatus: 저장 가능한 값은 PUBLISHED/HIDDEN/DELETED, 과거 VISIBLE 은 PUBLISHED 로 취급한다. */
class ReviewStatusTest {

    @Test
    void 저장_가능한_상태는_세가지이고_VISIBLE_은_포함하지_않는다() {
        assertThat(ReviewStatus.ALL).containsExactly("PUBLISHED", "HIDDEN", "DELETED")
                .doesNotContain(ReviewStatus.LEGACY_VISIBLE);
    }

    @Test
    void normalize_는_대소문자와_공백을_무시하고_VISIBLE_을_PUBLISHED_로_바꾼다() {
        assertThat(ReviewStatus.normalize(" hidden ")).isEqualTo("HIDDEN");
        assertThat(ReviewStatus.normalize("Visible")).isEqualTo("PUBLISHED");
        assertThatThrownBy(() -> ReviewStatus.normalize("ACTIVE")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReviewStatus.normalize(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isPublished_는_PUBLISHED_와_과거_VISIBLE_만_참이다() {
        assertThat(ReviewStatus.isPublished("published")).isTrue();
        assertThat(ReviewStatus.isPublished("VISIBLE")).isTrue();
        assertThat(ReviewStatus.isPublished("HIDDEN")).isFalse();
        assertThat(ReviewStatus.isPublished(null)).isFalse();
    }
}
