package com.honeyrest.honeyrest_user.service.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 메일 발송 on/off 판단: app.mail.enabled 명시값 우선, 없으면 spring.mail.username 유무.
 */
class MailEnabledConditionTest {

    @Test
    @DisplayName("username 이 있으면 켜진다")
    void enabledWhenUsernamePresent() {
        MockEnvironment env = new MockEnvironment().withProperty("spring.mail.username", "honeyrest@gmail.com");
        assertThat(MailEnabledCondition.isMailEnabled(env)).isTrue();
    }

    @Test
    @DisplayName("username 이 비었거나 없으면 꺼진다")
    void disabledWhenUsernameBlankOrMissing() {
        assertThat(MailEnabledCondition.isMailEnabled(new MockEnvironment())).isFalse();
        assertThat(MailEnabledCondition.isMailEnabled(
                new MockEnvironment().withProperty("spring.mail.username", "  "))).isFalse();
    }

    @Test
    @DisplayName("해석되지 않는 placeholder 는 미설정으로 본다")
    void disabledWhenPlaceholderUnresolved() {
        MockEnvironment env = new MockEnvironment().withProperty("spring.mail.username", "${gmail.username}");
        assertThat(MailEnabledCondition.isMailEnabled(env)).isFalse();
    }

    @Test
    @DisplayName("app.mail.enabled 를 명시하면 username 보다 우선한다")
    void explicitFlagWins() {
        MockEnvironment off = new MockEnvironment()
                .withProperty("app.mail.enabled", "false")
                .withProperty("spring.mail.username", "honeyrest@gmail.com");
        assertThat(MailEnabledCondition.isMailEnabled(off)).isFalse();

        MockEnvironment on = new MockEnvironment().withProperty("app.mail.enabled", "true");
        assertThat(MailEnabledCondition.isMailEnabled(on)).isTrue();

        // 빈 문자열(MAIL_ENABLED 미설정)은 "명시하지 않음"으로 취급한다
        MockEnvironment blank = new MockEnvironment()
                .withProperty("app.mail.enabled", "")
                .withProperty("spring.mail.username", "honeyrest@gmail.com");
        assertThat(MailEnabledCondition.isMailEnabled(blank)).isTrue();
    }
}
