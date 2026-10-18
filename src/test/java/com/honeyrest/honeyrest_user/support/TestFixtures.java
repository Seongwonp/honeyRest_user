package com.honeyrest.honeyrest_user.support;

import com.honeyrest.domain.entity.*;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 통합 테스트용 데이터 생성기 (H2 / Testcontainers MySQL 공용).
 * 각 메서드는 자체 트랜잭션에서 커밋하므로 다른 스레드·트랜잭션에서도 보인다.
 * 테스트 클래스끼리 같은 DB(스프링 컨텍스트 캐시)를 공유하므로 이메일·예약번호·지역명은 매번 고유 값으로 만든다.
 */
public class TestFixtures {

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000);

    private final TransactionTemplate tx;
    private final EntityManager em;

    public TestFixtures(TransactionTemplate tx, EntityManager em) {
        this.tx = tx;
        this.em = em;
    }

    public static String unique(String prefix) {
        return prefix + SEQ.incrementAndGet();
    }

    private <T> T persist(T entity) {
        return tx.execute(status -> {
            em.persist(entity);
            return entity;
        });
    }

    public User user(int point) {
        String key = unique("u");
        return persist(User.builder()
                .email(key + "@test.honeyrest")
                .name("테스터" + key)
                .phone("01012345678")
                .point(point)
                .role("USER")
                .status("ACTIVE")
                .isVerified(true)
                .build());
    }

    /** 숙소 + 업체/카테고리/지역. 지역 이름(subRegion)은 regionName 으로 검색 테스트를 격리한다. */
    public Accommodation accommodation(String regionName, String status) {
        return tx.execute(s -> {
            Region main = Region.builder().name(regionName + "도").level(1).isPopular(false).build();
            em.persist(main);
            Region sub = Region.builder().name(regionName).level(2).parentId(main.getRegionId()).isPopular(false).build();
            em.persist(sub);
            AccommodationCategory category = AccommodationCategory.builder().name("호텔").sortOrder(1).build();
            em.persist(category);
            Company company = Company.builder()
                    .name("테스트 업체")
                    .businessNumber(unique("BN-"))
                    .ownerName("김업체")
                    .phone("02-000-0000")
                    .email("host@test.honeyrest")
                    .address("서울특별시 중구 업체로 1")
                    .status("APPROVED")
                    .build();
            em.persist(company);
            Accommodation accommodation = Accommodation.builder()
                    .company(company)
                    .category(category)
                    .mainRegion(main)
                    .subRegion(sub)
                    .name(unique(regionName + " 숙소 "))
                    .address(regionName + " 숙소로 100")
                    .latitude(new BigDecimal("37.500000"))
                    .longitude(new BigDecimal("127.000000"))
                    .checkInTime(LocalDateTime.of(2025, 1, 1, 15, 0))
                    .checkOutTime(LocalDateTime.of(2025, 1, 1, 11, 0))
                    .rating(new BigDecimal("0.0"))
                    .minPrice(new BigDecimal("100000.00"))
                    .status(status)
                    .build();
            em.persist(accommodation);
            return accommodation;
        });
    }

    public Room room(Long accommodationId, int totalRooms, int maxOccupancy, String status) {
        return tx.execute(s -> {
            Room room = Room.builder()
                    .accommodation(em.getReference(Accommodation.class, accommodationId))
                    .name(unique("객실 "))
                    .type("DOUBLE")
                    .price(new BigDecimal("100000.00"))
                    .maxOccupancy(maxOccupancy)
                    .standardOccupancy(Math.min(2, maxOccupancy))
                    .extraPersonFee(BigDecimal.ZERO)
                    .totalRooms(totalRooms)
                    .status(status)
                    .build();
            em.persist(room);
            return room;
        });
    }

    public AccommodationImage image(Long accommodationId, String url, int sortOrder) {
        return tx.execute(s -> {
            AccommodationImage image = AccommodationImage.builder()
                    .accommodation(em.getReference(Accommodation.class, accommodationId))
                    .imageUrl(url)
                    .imageType(sortOrder == 1 ? "MAIN" : "SUB")
                    .sortOrder(sortOrder)
                    .build();
            em.persist(image);
            return image;
        });
    }

    public CancellationPolicy cancellationPolicy(Long accommodationId) {
        return tx.execute(s -> {
            CancellationPolicy policy = CancellationPolicy.builder()
                    .accommodation(em.getReference(Accommodation.class, accommodationId))
                    .policyName("무료 취소")
                    .detail("체크인 3일 전까지 전액 환불")
                    .build();
            em.persist(policy);
            return policy;
        });
    }

    /** 기본: 정액 5,000원, 최소 주문 없음, 전체 대상, 활성, 기간 유효. customizer 로 바꾼다. */
    public Coupon coupon(Consumer<Coupon.CouponBuilder> customizer) {
        Coupon.CouponBuilder b = Coupon.builder()
                .name(unique("쿠폰 "))
                .code(UUID.randomUUID().toString().substring(0, 20))
                .discountType("FIXED")
                .discountValue(new BigDecimal("5000.00"))
                .targetType("ALL")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .isActive(true);
        customizer.accept(b);
        return persist(b.build());
    }

    public UserCoupon userCoupon(Long userId, Long couponId, String status) {
        return tx.execute(s -> {
            UserCoupon userCoupon = UserCoupon.builder()
                    .user(em.getReference(User.class, userId))
                    .coupon(em.getReference(Coupon.class, couponId))
                    .status(status)
                    .issuedAt(LocalDateTime.now().minusDays(1))
                    .expiredAt(LocalDateTime.now().plusDays(30))
                    .build();
            em.persist(userCoupon);
            return userCoupon;
        });
    }

    public Reservation reservation(Long userId, Long roomId, String status, LocalDate checkIn, LocalDate checkOut) {
        return tx.execute(s -> {
            Room room = em.find(Room.class, roomId);
            Reservation reservation = Reservation.builder()
                    .user(userId != null ? em.getReference(User.class, userId) : null)
                    .room(room)
                    .accommodation(room.getAccommodation())
                    .accommodationName(room.getAccommodation().getName())
                    .roomName(room.getName())
                    .reservationNumber(unique("RES-FX-"))
                    .checkInDate(checkIn)
                    .checkOutDate(checkOut)
                    .guestCount(2)
                    .guestName("홍길동")
                    .guestPhone("01012345678")
                    .price(new BigDecimal("100000.00"))
                    .originalPrice(new BigDecimal("100000.00"))
                    .discountAmount(BigDecimal.ZERO)
                    .status(status)
                    .build();
            em.persist(reservation);
            return reservation;
        });
    }

    public Inquiry inquiry(Long userId, Long accommodationId, String category, String reply) {
        return tx.execute(s -> {
            Inquiry inquiry = Inquiry.builder()
                    .user(em.getReference(User.class, userId))
                    .accommodation(em.getReference(Accommodation.class, accommodationId))
                    .title("원래 제목")
                    .content("원래 내용")
                    .category(category)
                    .reply(reply)
                    .isReplied(reply != null)
                    .build();
            em.persist(inquiry);
            return inquiry;
        });
    }

    public <T> T find(Class<T> type, Object id) {
        return tx.execute(s -> em.find(type, id));
    }
}
