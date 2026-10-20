package com.honeyrest.honeyrest_user.service.email;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 실제 메일(SMTP) 발송을 켤지 결정하는 조건.
 * {@code app.mail.enabled} 를 명시하면 그 값을 따르고, 없으면 {@code spring.mail.username} 이 있을 때만 켠다.
 */
public class MailEnabledCondition extends SpringBootCondition {

    static final String ENABLED_PROPERTY = "app.mail.enabled";
    static final String USERNAME_PROPERTY = "spring.mail.username";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return isMailEnabled(context.getEnvironment())
                ? ConditionOutcome.match("메일 발송 활성화")
                : ConditionOutcome.noMatch("메일 발송 비활성화 (app.mail.enabled=false 또는 spring.mail.username 비어 있음)");
    }

    static boolean isMailEnabled(Environment env) {
        String explicit = safeGet(env, ENABLED_PROPERTY);
        if (explicit != null && !explicit.isBlank()) {
            return Boolean.parseBoolean(explicit.trim());
        }
        String username = safeGet(env, USERNAME_PROPERTY);
        // 치환되지 않은 ${...} 가 그대로 남은 경우도 "미설정"으로 본다.
        return username != null && !username.isBlank() && !username.contains("${");
    }

    /** 해석할 수 없는 placeholder 가 있어도 예외 대신 null 을 돌려준다 (미설정으로 취급). */
    private static String safeGet(Environment env, String key) {
        try {
            return env.getProperty(key);
        } catch (IllegalArgumentException unresolvable) {
            return null;
        }
    }

    /** {@link MailEnabledCondition} 의 반대 조건 (메일 비활성화 시 매칭). */
    public static class Disabled extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return isMailEnabled(context.getEnvironment())
                    ? ConditionOutcome.noMatch("메일 발송 활성화 상태")
                    : ConditionOutcome.match("메일 발송 비활성화 → no-op 발송기 사용");
        }
    }
}
