package com.honeyrest.domain.entity;


import com.honeyrest.domain.type.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reservation", indexes = {
        @Index(name = "idx_reservation_user_id", columnList = "user_id"),
        @Index(name = "idx_reservation_status", columnList = "status"),
        @Index(name = "idx_reservation_check_in_date", columnList = "check_in_date"),
        @Index(name = "idx_reservation_check_out_date", columnList = "check_out_date")
})
public class Reservation extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id",  nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    // 숙소명 스냅샷 (V10, NOT NULL). 예약 생성 시 반드시 채운다.
    @Column(name = "accommodation_name", nullable = false, length = 255)
    private String accommodationName;

    @Column(name = "room_name", nullable = false, length = 255)
    private String roomName; // 객실명

    @Column(name = "reservation_number",nullable = false, unique = true, length = 50 )
    private String reservationNumber; // 고유 예약 번호

    @Column(name = "check_in_date",nullable = false)
    private LocalDate checkInDate; // 체크인

    @Column(name = "check_out_date" ,nullable = false)
    private LocalDate checkOutDate; // 체크 아웃

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;

    @Column(name = "guest_phone", nullable = false, length = 20)
    private String guestPhone;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // 최종 결제 금액 (스키마 DECIMAL(10,2) — 사용자 사본에 precision 누락)

    @Column(name = "original_price",precision = 10, scale = 2)
    private BigDecimal originalPrice; // 할인 전 원가

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount; // 총 할인 금액

    @Setter
    @Column(name = "status", nullable = false, length = 20)
    private String status; // 예약 상태

    @Setter
    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason; // 취소 사유

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequest; // 특별 요청 사항

    // 공유 스키마(Flyway)에 version 컬럼이 없어 @Version 필드를 두지 않는다.
    // 동시성은 예약 생성/점유 전환 시 room 행 비관적 락으로 제어한다 (사용자/호스트 각 서비스).

    // ===== 이하 호스트 사본에서 합친 도메인 메서드 =====

    /** 생성 시 필수값 검증 (엔티티 내부). */
    public void validateNew() {
        if (user == null)               throw new IllegalStateException("user는 필수입니다.");
        if (room == null)               throw new IllegalStateException("room은 필수입니다.");
        if (accommodation == null)      throw new IllegalStateException("accommodationId는 필수입니다.");
        if (accommodationName == null)  throw new IllegalStateException("accommodationName은 필수입니다.");
        if (roomName == null)           throw new IllegalStateException("roomName은 필수입니다.");
        if (reservationNumber == null)  throw new IllegalStateException("reservationNumber는 필수입니다.");
        if (checkInDate == null)        throw new IllegalStateException("checkInDate는 필수입니다.");
        if (checkOutDate == null)       throw new IllegalStateException("checkOutDate는 필수입니다.");
        if (guestCount == null)         throw new IllegalStateException("guestCount는 필수입니다.");
        if (guestName == null)          throw new IllegalStateException("guestName은 필수입니다.");
        if (guestPhone == null)         throw new IllegalStateException("guestPhone는 필수입니다.");
        if (price == null)              throw new IllegalStateException("price는 필수입니다.");
        if (status == null)             throw new IllegalStateException("status는 필수입니다.");
        validateDates();
    }

    private void validateDates() {
        if (checkInDate != null && checkOutDate != null && !checkInDate.isBefore(checkOutDate)) {
            throw new IllegalArgumentException("체크인 날짜는 체크아웃 이전이어야 합니다.");
        }
    }

    /**
     * 부분 업데이트: null 이 아닌 값만 반영한다.
     * [통합] 호스트 사본은 호스트 ReservationDTO 를 직접 받았다. 공유 모듈은 앱 DTO 에 의존할 수 없어
     * {@link ReservationChanges} 인터페이스로 받는다 (호스트 ReservationDTO 가 구현).
     * 예약번호(reservationNumber)는 불변으로 취급해 바꾸지 않는다.
     */
    public void update(ReservationChanges changes, User newUser, Room newRoom, Accommodation newAccommodation) {
        if (changes.getCheckInDate() != null)  this.checkInDate = changes.getCheckInDate();
        if (changes.getCheckOutDate() != null) this.checkOutDate = changes.getCheckOutDate();
        validateDates();

        if (changes.getGuestCount() != null)     this.guestCount = changes.getGuestCount();
        if (changes.getGuestName() != null)      this.guestName = changes.getGuestName();
        if (changes.getGuestPhone() != null)     this.guestPhone = changes.getGuestPhone();
        if (changes.getPrice() != null)          this.price = changes.getPrice();
        if (changes.getStatus() != null)         this.status = changes.getStatus();
        if (changes.getCancelReason() != null)   this.cancelReason = changes.getCancelReason();
        if (changes.getSpecialRequest() != null) this.specialRequest = changes.getSpecialRequest();
        if (changes.getAccommodationName() != null) this.accommodationName = changes.getAccommodationName();

        if (newUser != null) this.user = newUser;
        if (newRoom != null) this.room = newRoom;
        if (newAccommodation != null) this.accommodation = newAccommodation;
    }

    /** 예약 취소. 이미 취소된 예약이면 IllegalStateException. */
    public void cancel(String reason) {
        if (ReservationStatus.CANCELLED.equalsIgnoreCase(this.status)) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancelReason = reason;
    }
}
