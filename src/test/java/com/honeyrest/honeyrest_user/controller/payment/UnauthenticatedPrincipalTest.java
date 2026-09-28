package com.honeyrest.honeyrest_user.controller.payment;

import com.honeyrest.honeyrest_user.controller.auth.EmailController;
import com.honeyrest.honeyrest_user.controller.review.ReviewController;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.service.ReviewService;
import com.honeyrest.honeyrest_user.service.email.EmailVerificationTokenService;
import com.honeyrest.honeyrest_user.service.payment.PaymentOrchestrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * principal 이 없을 때(미인증) 컨트롤러가 403 이 아니라 401 을 내는지 확인한다.
 * 과거에는 AccessDeniedException("로그인이 필요합니다") 를 던져 GlobalExceptionHandler 가 403 으로 매핑했다.
 */
@ExtendWith(MockitoExtension.class)
class UnauthenticatedPrincipalTest {

    @Mock private PaymentOrchestrationService paymentOrchestrationService;
    @Mock private EmailVerificationTokenService emailVerificationTokenService;
    @Mock private ReviewService reviewService;

    private static void assertUnauthorized(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 결제_승인_미인증은_401() {
        PaymentController controller = new PaymentController(paymentOrchestrationService);

        assertUnauthorized(() -> controller.confirmTossPayment(null, new TossConfirmRequest()));
        verifyNoInteractions(paymentOrchestrationService);
    }

    @Test
    void 이메일_변경_확정_미인증은_401() {
        EmailController controller = new EmailController(emailVerificationTokenService);

        assertUnauthorized(() -> controller.verifyEmailChange(null, "token", "new@example.com"));
        verifyNoInteractions(emailVerificationTokenService);
    }

    @Test
    void 리뷰_좋아요_미인증은_401() {
        ReviewController controller = new ReviewController(reviewService);

        assertUnauthorized(() -> controller.toggleLike(null, 1L, false));
        verifyNoInteractions(reviewService);
    }
}
