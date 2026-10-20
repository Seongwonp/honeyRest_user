package com.honeyrest.honeyrest_user.e2e;

import com.honeyrest.domain.entity.EmailVerificationToken;
import com.honeyrest.domain.entity.Reservation;
import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.repository.EmailVerificationTokenRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * E2E 테스트 보조 엔드포인트. e2e 프로필에서만 등록된다 (demo 포함 다른 프로필에는 {@code /e2e/**} 가 없다).
 * <ul>
 *     <li>{@code GET /e2e/verification-token?email=} — 가장 최근 가입 인증 토큰</li>
 *     <li>{@code POST /e2e/reservations/{id}/complete} — 예약을 COMPLETED 로 전환</li>
 *     <li>{@code GET /e2e/toss/cancellations} — 토스 스텁에 기록된 결제 취소 요청 목록</li>
 * </ul>
 */
@Log4j2
@RestController
@Profile("e2e")
@RequestMapping("/e2e")
@RequiredArgsConstructor
public class E2eSupportController {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final ReservationRepository reservationRepository;
    private final E2eTossClient tossClient;

    @GetMapping("/verification-token")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> latestVerificationToken(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "사용자가 없습니다."));
        }
        return tokenRepository.findByUser(user).stream()
                .filter(t -> t.getTokenType() == null || "SIGNUP".equals(t.getTokenType()))
                .max(Comparator.comparing(EmailVerificationToken::getTokenId))
                .map(t -> ResponseEntity.ok(Map.of("token", t.getTokenValue())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "가입 인증 토큰이 없습니다.")));
    }

    @PostMapping("/reservations/{reservationId}/complete")
    @Transactional
    public ResponseEntity<Map<String, Object>> completeReservation(@PathVariable Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "예약이 없습니다."));
        }
        // CONFIRMED 가 아니면 IllegalStateException → 409 (GlobalExceptionHandler)
        reservation.complete();
        log.info("[e2e] 예약 이용 완료 처리: reservationId={}", reservationId);
        return ResponseEntity.ok(Map.of("reservationId", reservationId, "status", reservation.getStatus()));
    }

    @GetMapping("/toss/cancellations")
    public List<E2eTossClient.Cancellation> tossCancellations() {
        return tossClient.cancellations();
    }
}
