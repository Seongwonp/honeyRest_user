package com.honeyrest.honeyrest_user.service.reservation;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.honeyrest_user.dto.reservation.guest.GuestReservationLookupRequestDTO;
import com.honeyrest.honeyrest_user.entity.*;
import com.honeyrest.honeyrest_user.mapper.ReservationMapper;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.payment.PaymentDetailRepository;
import com.honeyrest.honeyrest_user.repository.payment.PaymentRepository;
import com.honeyrest.honeyrest_user.repository.reservation.ReservationRepository;
import com.honeyrest.honeyrest_user.repository.review.ReviewRepository;
import com.honeyrest.honeyrest_user.repository.room.RoomRepository;
import com.honeyrest.honeyrest_user.service.PointHistoryService;
import com.honeyrest.honeyrest_user.service.UserService;
import com.honeyrest.honeyrest_user.service.coupon.CouponUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ReserveService 테스트")
class ReserveServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentDetailRepository paymentDetailRepository;
    @Mock private ReservationMapper reservationMapper;
    @Mock private ReviewRepository reviewRepository;
    @Mock private UserService userService;
    @Mock private CouponUsageService couponUsageService;
    @Mock private PointHistoryService pointHistoryService;

    @InjectMocks
    private ReserveService reserveService;

    private Accommodation accommodation;
    private Room room;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        accommodation = Accommodation.builder()
                .accommodationId(1L)
                .name("테스트 숙소")
                .build();

        room = Room.builder()
                .roomId(10L)
                .name("디럭스")
                .accommodation(accommodation)
                .price(BigDecimal.valueOf(100000))
                .maxOccupancy(4)
                .standardOccupancy(2)
                .build();
    }

    // ── createReservation ──────────────────────────────────

    @Test
    @DisplayName("비회원 예약 생성 성공")
    void createReservation_guestSuccess() {
        ReservationRequestDTO request = ReservationRequestDTO.builder()
                .roomId(10L)
                .checkIn(LocalDate.now().plusDays(1))
                .checkOut(LocalDate.now().plusDays(3))
                .guests(2)
                .guestName("홍길동")
                .guestPhone("01012345678")
                .reservationCode("RES-001")
                .build();

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        Reservation saved = Reservation.builder()
                .reservationId(1L)
                .room(room)
                .accommodation(accommodation)
                .reservationNumber("RES-001")
                .status("CONFIRMED")
                .price(BigDecimal.valueOf(200000))
                .build();
        when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

        Reservation result = reserveService.createReservation(request, BigDecimal.valueOf(200000), BigDecimal.ZERO);

        assertThat(result.getReservationNumber()).isEqualTo("RES-001");
        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("존재하지 않는 객실 예약 시 예외 발생")
    void createReservation_roomNotFound() {
        ReservationRequestDTO request = ReservationRequestDTO.builder()
                .roomId(999L)
                .checkIn(LocalDate.now().plusDays(1))
                .checkOut(LocalDate.now().plusDays(2))
                .guests(1)
                .guestName("홍길동")
                .guestPhone("01012345678")
                .reservationCode("RES-002")
                .build();

        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reserveService.createReservation(request, BigDecimal.valueOf(100000), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 객실");
    }

    // ── findGuestReservation ───────────────────────────────

    @Test
    @DisplayName("비회원 예약 조회 성공")
    void findGuestReservation_success() {
        GuestReservationLookupRequestDTO request = GuestReservationLookupRequestDTO.builder()
                .reservationCode("RES-001")
                .guestPhone("01012345678")
                .guestPassword("pass")
                .build();

        Reservation reservation = Reservation.builder()
                .reservationId(1L)
                .room(room)
                .accommodation(accommodation)
                .reservationNumber("RES-001")
                .status("CONFIRMED")
                .price(BigDecimal.valueOf(200000))
                .build();

        Payment payment = Payment.builder()
                .paymentId(1L)
                .paymentMethod("CARD")
                .paymentStatus("DONE")
                .amount(BigDecimal.valueOf(200000))
                .build();

        when(reservationRepository.findByReservationNumberAndGuestPhone("RES-001", "01012345678"))
                .thenReturn(reservation);
        when(paymentRepository.findByReservation(reservation)).thenReturn(Optional.of(payment));
        when(reservationMapper.toCompleteDTO(reservation, payment)).thenReturn(
                com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO.builder()
                        .reservationId(1L)
                        .reservationCode("RES-001")
                        .build()
        );

        var result = reserveService.findGuestReservation(request);

        assertThat(result.getReservationCode()).isEqualTo("RES-001");
    }

    @Test
    @DisplayName("비회원 예약 조회 실패 - 예약 없음")
    void findGuestReservation_notFound() {
        GuestReservationLookupRequestDTO request = GuestReservationLookupRequestDTO.builder()
                .reservationCode("WRONG")
                .guestPhone("01000000000")
                .guestPassword("pass")
                .build();

        when(reservationRepository.findByReservationNumberAndGuestPhone("WRONG", "01000000000"))
                .thenReturn(null);

        assertThatThrownBy(() -> reserveService.findGuestReservation(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("예약 정보를 찾을 수 없습니다");
    }

    // ── requestReservationCancel ───────────────────────────

    @Test
    @DisplayName("예약 취소 요청 성공")
    void requestReservationCancel_success() {
        Reservation reservation = Reservation.builder()
                .reservationId(1L)
                .status("CONFIRMED")
                .room(room)
                .accommodation(accommodation)
                .price(BigDecimal.valueOf(100000))
                .build();

        when(reservationRepository.findByReservationIdAndUser_UserId(1L, 1L))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        assertThatCode(() -> reserveService.requestReservationCancel(1L, 1L, "여행 일정 변경"))
                .doesNotThrowAnyException();

        assertThat(reservation.getStatus()).isEqualTo("CANCEL_REQUEST");
    }

    @Test
    @DisplayName("CONFIRMED 아닌 예약 취소 요청 시 예외 발생")
    void requestReservationCancel_invalidStatus() {
        Reservation reservation = Reservation.builder()
                .reservationId(1L)
                .status("CANCEL_REQUEST")
                .room(room)
                .accommodation(accommodation)
                .price(BigDecimal.valueOf(100000))
                .build();

        when(reservationRepository.findByReservationIdAndUser_UserId(1L, 1L))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reserveService.requestReservationCancel(1L, 1L, "사유"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("확정된 예약만");
    }
}
