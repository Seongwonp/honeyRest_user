package com.honeyrest.honeyrest_user.e2e;

import com.honeyrest.domain.entity.EmailVerificationToken;
import com.honeyrest.domain.entity.Reservation;
import com.honeyrest.domain.entity.User;
import com.honeyrest.domain.type.ReservationStatus;
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
 * E2E 테스트 보조 엔드포인트 — <b>e2e 프로필에서만 존재한다.</b>
 * <p>
 * {@code @Profile("e2e")} 이므로 local/test/운영 등 다른 모든 프로필에서는 이 컨트롤러 빈이 만들어지지 않아
 * {@code /e2e/**} 경로 자체가 없다(404). 경로가 {@code /api/**} 밖이라 SecurityConfig 의 {@code anyRequest().permitAll()}
 * 규칙을 타지만, 빈이 없는 프로필에서는 매핑될 핸들러가 없으므로 노출되지 않는다.
 * e2e 프로필은 H2 인메모리 DB·스텁 결제·메일 미발송 전용이므로 실제 사용자 데이터에 닿을 수 없다.
 * <ul>
 *     <li>{@code GET /e2e/verification-token?email=} — 가장 최근 가입 인증(SIGNUP) 토큰. 메일 대신 테스트가 사용.</li>
 *     <li>{@code POST /e2e/reservations/{id}/complete} — 예약을 COMPLETED(이용 완료)로 전환. 원래는 호스트 앱/체크아웃이 담당.</li>
 *     <li>{@code GET /e2e/toss/cancellations} — 토스 스텁에 기록된 결제 취소 요청 목록.</li>
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
        reservation.setStatus(ReservationStatus.COMPLETED);
        log.info("[e2e] 예약 이용 완료 처리: reservationId={}", reservationId);
        return ResponseEntity.ok(Map.of("reservationId", reservationId, "status", reservation.getStatus()));
    }

    @GetMapping("/toss/cancellations")
    public List<E2eTossClient.Cancellation> tossCancellations() {
        return tossClient.cancellations();
    }
}
