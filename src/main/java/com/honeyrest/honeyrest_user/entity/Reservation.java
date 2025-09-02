package com.honeyrest.honeyrest_user.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reservation")
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

    @Column(name = "price", nullable = false )
    private BigDecimal price; // 최종 결제 금액

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

}
