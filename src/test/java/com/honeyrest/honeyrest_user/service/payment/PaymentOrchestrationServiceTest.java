package com.honeyrest.honeyrest_user.service.payment;

import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.dto.reservation.PriceBreakdownDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.domain.entity.Payment;
import com.honeyrest.domain.entity.Reservation;
import com.honeyrest.domain.entity.Room;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.mapper.ReservationMapper;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.coupon.UserCouponRepository;
import com.honeyrest.honeyrest_user.repository.payment.PaymentRepository;
import com.honeyrest.honeyrest_user.repository.reservation.ReservationRepository;
import com.honeyrest.honeyrest_user.repository.room.RoomRepository;
import com.honeyrest.honeyrest_user.service.email.EmailService;
import com.honeyrest.honeyrest_user.service.reservation.PriceCalculator;
import com.honeyrest.honeyrest_user.service.reservation.ReserveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("PaymentOrchestrationService 테스트")
class PaymentOrchestrationServiceTest {

    private static final String PAYMENT_KEY = "pk_test";
    private static final String ORDER_ID = "RES-100";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(200000);
    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(1);
    private static final LocalDate CHECK_OUT = LocalDate.now().plusDays(3);

    @Mock private TossService tossService;
    @Mock private ReserveService reserveService;
    @Mock private PaymentService paymentService;
    @Mock private PaymentDetailService paymentDetailService;
    @Mock private ReservationMapper reservationMapper;
    @Mock private EmailService emailService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private RoomRepository roomRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserCouponRepository userCouponRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private PriceCalculator priceCalculator;

    @InjectMocks
    private PaymentOrchestrationService service;

    private Room room;
    private TossConfirmRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        room = Room.builder()
                .roomId(10L)
                .name("디럭스")
                .price(BigDecimal.valueOf(100000))
                .standardOccupancy(2)
                .maxOccupancy(4)
                .totalRooms(1)
                .build();

        ReservationRequestDTO info = ReservationRequestDTO.builder()
                .roomId(10L)
                .checkIn(CHECK_IN)
                .checkOut(CHECK_OUT)
                .guests(2)
                .guestName("홍길동")
                .guestPhone("01012345678")
                .reservationCode(ORDER_ID)
                .build();
        request = TossConfirmRequest.builder()
                .paymentKey(PAYMENT_KEY)
                .orderId(ORDER_ID)
                .amount(AMOUNT)
                .reservationInfo(info)
                .build();

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(priceCalculator.calculate(room, CHECK_IN, CHECK_OUT, 2)).thenReturn(new PriceBreakdownDTO(
                2, List.of(), AMOUNT, 0, BigDecimal.ZERO, BigDecimal.ZERO, AMOUNT));
        when(tossService.confirmPayment(request)).thenReturn(TossPaymentResult.builder()
                .paymentKey(PAYMENT_KEY)
                .orderId(ORDER_ID)
                .status("DONE")
                .amount(AMOUNT)
                .reservationInfo(info)
                .build());
        when(transactionTemplate.execute(any())).thenAnswer(inv ->
                inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    }

    @Test
    @DisplayName("정상 흐름: 승인 후 저장되고 결제 취소는 호출되지 않는다")
    void success() {
        Reservation reservation = Reservation.builder().reservationId(1L).build();
        Payment payment = Payment.builder().paymentId(1L).build();
        when(reserveService.createReservation(any(), eq(AMOUNT), any())).thenReturn(reservation);
        when(paymentService.saveTossPayment(any(), eq(reservation))).thenReturn(payment);
        when(reservationMapper.toCompleteDTO(reservation, payment))
                .thenReturn(ReservationCompleteDTO.builder().reservationCode(ORDER_ID).build());

        ReservationCompleteDTO dto = service.confirmAndSave(request);

        assertThat(dto.getReservationCode()).isEqualTo(ORDER_ID);
        verify(tossService, never()).cancelPayment(anyString(), anyString());
        assertThat(request.getReservationInfo().getOriginalPrice()).isEqualByComparingTo(AMOUNT);
    }

    @Test
    @DisplayName("재고가 없으면 토스 승인 호출 전에 409로 거절한다 (과금 없음)")
    void overlapRejectedBeforeTossConfirm() {
        doThrow(new ApiException("선택하신 날짜에 예약 가능한 객실이 없습니다.", HttpStatus.CONFLICT))
                .when(reserveService).assertRoomAvailable(room, CHECK_IN, CHECK_OUT);

        assertThatThrownBy(() -> service.confirmAndSave(request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verifyNoInteractions(tossService);
        verify(reserveService, never()).createReservation(any(), any(), any());
    }

    @Test
    @DisplayName("이미 저장된 paymentKey 면 토스 승인 전에 409로 거절한다")
    void duplicatePaymentRejectedBeforeTossConfirm() {
        when(paymentRepository.existsByTransactionId(PAYMENT_KEY)).thenReturn(true);

        assertThatThrownBy(() -> service.confirmAndSave(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("이미 처리된 결제");

        verifyNoInteractions(tossService);
    }

    @Test
    @DisplayName("승인 후 저장이 실패하면 결제를 취소하고 500 ApiException(취소 안내)으로 다시 던진다")
    void persistenceFailureTriggersCancel() {
        when(reserveService.createReservation(any(), any(), any())).thenThrow(new RuntimeException("DB 오류"));

        assertThatThrownBy(() -> service.confirmAndSave(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("결제는 자동으로 취소되었습니다")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(tossService).confirmPayment(request);
        verify(tossService).cancelPayment(eq(PAYMENT_KEY), anyString());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("락 안의 재고 재확인에서 매진되면 결제를 취소하고 409를 유지한다")
    void soldOutInsideLockCancelsWith409() {
        when(reserveService.createReservation(any(), any(), any()))
                .thenThrow(new ApiException("선택하신 날짜에 예약 가능한 객실이 없습니다.", HttpStatus.CONFLICT));

        assertThatThrownBy(() -> service.confirmAndSave(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("예약 가능한 객실이 없습니다")
                .hasMessageContaining("자동으로 취소")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(tossService).cancelPayment(eq(PAYMENT_KEY), anyString());
    }

    @Test
    @DisplayName("결제 취소까지 실패하면 고객센터 안내 500을 던진다")
    void cancelFailure() {
        when(reserveService.createReservation(any(), any(), any())).thenThrow(new RuntimeException("DB 오류"));
        doThrow(new ApiException("토스 결제 취소 실패", HttpStatus.BAD_GATEWAY))
                .when(tossService).cancelPayment(eq(PAYMENT_KEY), anyString());

        assertThatThrownBy(() -> service.confirmAndSave(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("고객센터")
                .hasMessageContaining(ORDER_ID)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("같은 paymentKey 가 동시 요청으로 이미 저장됐다면 결제를 취소하지 않는다")
    void noCancelWhenAlreadyPersistedByConcurrentRequest() {
        // 사전 확인 시점에는 없음 → 트랜잭션 안에서 발견 → 보상 시점에도 존재
        when(paymentRepository.existsByTransactionId(PAYMENT_KEY)).thenReturn(false, true, true);

        assertThatThrownBy(() -> service.confirmAndSave(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("이미 처리된 결제");

        verify(tossService, never()).cancelPayment(anyString(), anyString());
        verify(reserveService, never()).createReservation(any(), any(), any());
    }
}
