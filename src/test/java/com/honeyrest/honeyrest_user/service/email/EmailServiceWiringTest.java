package com.honeyrest.honeyrest_user.service.email;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.e2e.E2eNoOpEmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 메일 발송 포트({@link EmailService}) 배선: 프로필/조건마다 구현이 정확히 하나만 등록되고,
 * no-op 구현은 JavaMailSender 를 전혀 건드리지 않는지 확인한다.
 */
@DisplayName("EmailService 구현은 프로필/조건별로 하나만 등록된다")
class EmailServiceWiringTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(JavaMailSender.class, () -> mailSender)
            .withUserConfiguration(SmtpEmailService.class, NoOpEmailService.class, E2eNoOpEmailService.class);

    @Test
    @DisplayName("메일 설정이 있으면 SMTP 구현")
    void smtpWhenMailEnabled() {
        runner.withPropertyValues("spring.mail.username=honeyrest@gmail.com").run(ctx -> {
            assertThat(ctx).hasSingleBean(EmailService.class);
            assertThat(ctx.getBean(EmailService.class)).isInstanceOf(SmtpEmailService.class);
        });
    }

    @Test
    @DisplayName("메일 설정이 없으면 NoOp 구현이고 JavaMailSender 를 쓰지 않는다")
    void noOpWhenMailDisabled() {
        runner.withPropertyValues("app.mail.enabled=false", "spring.mail.username=honeyrest@gmail.com").run(ctx -> {
            assertThat(ctx).hasSingleBean(EmailService.class);
            EmailService service = ctx.getBean(EmailService.class);
            assertThat(service).isInstanceOf(NoOpEmailService.class);

            sendAll(service);
            verifyNoInteractions(mailSender);
        });
    }

    @Test
    @DisplayName("e2e 프로필이면 메일 설정과 무관하게 e2e NoOp 구현이고 JavaMailSender 를 쓰지 않는다")
    void e2eNoOpOnE2eProfile() {
        runner.withPropertyValues("spring.profiles.active=e2e", "spring.mail.username=e2e@example.com").run(ctx -> {
            assertThat(ctx).hasSingleBean(EmailService.class);
            EmailService service = ctx.getBean(EmailService.class);
            assertThat(service).isInstanceOf(E2eNoOpEmailService.class);

            sendAll(service);
            verifyNoInteractions(mailSender);
        });
    }

    @Test
    @DisplayName("NoOp 구현은 SMTP 구현을 상속하지 않고 JavaMailSender 필드도 없다")
    void noOpImplementationsDoNotDependOnMailSender() {
        for (Class<?> type : new Class<?>[]{NoOpEmailService.class, E2eNoOpEmailService.class}) {
            assertThat(SmtpEmailService.class.isAssignableFrom(type)).as(type.getSimpleName()).isFalse();
            assertThat(Arrays.stream(type.getDeclaredFields()).map(Field::getType))
                    .as(type.getSimpleName())
                    .doesNotContain(JavaMailSender.class);
        }
    }

    private static void sendAll(EmailService service) {
        // 아무것도 읽거나 보내지 않으므로 예외 없이 끝나야 한다.
        service.sendReservationConfirmation("a@example.com", new ReservationCompleteDTO());
        service.sendPasswordReset("a@example.com", "token");
        service.sendVerificationEmail("a@example.com", "https://example.com/verify");
        service.sendEmailChangeToken("a@example.com", "https://example.com/change");
    }
}
