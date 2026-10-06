package com.honeyrest.honeyrest_user.service.email;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 실제 메일(SMTP) 발송을 켤지 결정하는 조건.
 * <ol>
 *   <li>{@code app.mail.enabled} 를 명시하면 그 값(true/false)을 그대로 따른다.</li>
 *   <li>명시하지 않으면 {@code spring.mail.username} 이 채워져 있을 때만 켠다.
 *       (운영 prod 프로필은 {@code MAIL_USERNAME} 환경변수가 비어 있으면 빈 값이 된다)</li>
 * </ol>
 * 꺼지면 {@link EmailService} 대신 {@link NoOpEmailService} 가 등록되어, Gmail 앱 비밀번호 없이도
 * 기동·가입·결제가 실패하지 않는다(메일만 발송되지 않음).
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
