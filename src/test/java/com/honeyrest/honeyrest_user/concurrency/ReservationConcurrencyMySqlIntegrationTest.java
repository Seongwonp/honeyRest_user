package com.honeyrest.honeyrest_user.concurrency;

import com.honeyrest.domain.entity.*;
import com.honeyrest.domain.type.ReservationStatus;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.repository.reservation.ReservationRepository;
import com.honeyrest.honeyrest_user.service.PointService;
import com.honeyrest.honeyrest_user.service.coupon.CouponUsageService;
import com.honeyrest.honeyrest_user.service.reservation.ReserveService;
import com.honeyrest.honeyrest_user.support.ConcurrencyResult;
import com.honeyrest.honeyrest_user.support.InMemoryRedisTestConfig;
import com.honeyrest.honeyrest_user.support.TestFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 MySQL 8.0(InnoDB) 에서 예약·쿠폰·포인트 동시성을 검증한다. H2 는 MVCC/잠금 동작이 달라 이 경쟁을 재현하지 못한다.
 * <ul>
 *   <li>total_rooms=1 객실에 동시 예약 → 정확히 1건만 성공 (나머지 409)</li>
 *   <li>REPEATABLE READ 에서 객실 락 전에 스냅샷이 고정돼도 방금 커밋된 예약을 놓치지 않는다 (잠금 읽기 회귀)</li>
 *   <li>같은 쿠폰/같은 포인트 잔액 동시 사용 → 하나만 성공</li>
 * </ul>
 * Docker 가 없으면 건너뛴다. 실행: {@code ./gradlew integrationTest}
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Import(InMemoryRedisTestConfig.class)
@DisplayName("예약/쿠폰/포인트 동시성 (MySQL 8.0)")
class ReservationConcurrencyMySqlIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("honeyrest_db")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "12");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // 운영과 같은 격리 수준(application.properties 의 READ COMMITTED)을 그대로 쓴다.
        // REPEATABLE READ(MySQL 기본값) 경우는 아래 스냅샷 테스트에서 트랜잭션 단위로 강제한다.
    }

    @Autowired private ReserveService reserveService;
    @Autowired private CouponUsageService couponUsageService;
    @Autowired private PointService pointService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    private TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(transactionTemplate, entityManager);
    }

    private ReservationRequestDTO request(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        return ReservationRequestDTO.builder()
                .roomId(roomId)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guests(2)
                .guestName("홍길동")
                .guestPhone("01012345678")
                .reservationCode(TestFixtures.unique("RES-IT-"))
                .originalPrice(new BigDecimal("100000.00"))
                .build();
    }

    private long occupying(Long roomId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservation WHERE room_id = ? AND status IN ('CONFIRMED','PENDING','CANCEL_REQUEST')",
                Long.class, roomId);
        return count != null ? count : 0;
    }

    @Test
    @DisplayName("total_rooms=1 객실에 8건이 동시에 예약하면 정확히 1건만 성공하고 나머지는 409")
    void concurrentReservationsForSingleRoom() throws Exception {
        Accommodation acc = fixtures.accommodation(TestFixtures.unique("동시성시"), "ACTIVE");
        Room room = fixtures.room(acc.getAccommodationId(), 1, 4, "ACTIVE");
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        ConcurrencyResult result = ConcurrencyResult.run(8, i -> reserveService.createReservation(
                request(room.getRoomId(), checkIn, checkOut), new BigDecimal("200000.00"), BigDecimal.ZERO));

        assertThat(result.successes()).isEqualTo(1);
        assertThat(result.failures()).hasSize(7).allSatisfy(e -> assertThat(e)
                .isInstanceOf(ApiException.class)
                .extracting(x -> ((ApiException) x).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(occupying(room.getRoomId())).isEqualTo(1);
    }

    @Test
    @DisplayName("REPEATABLE READ: 객실 락 전에 스냅샷이 고정된 트랜잭션도 방금 커밋된 예약을 보고 409 로 거절한다")
    void snapshotTakenBeforeLockStillSeesCommittedReservation() throws Exception {
        Accommodation acc = fixtures.accommodation(TestFixtures.unique("스냅샷시"), "ACTIVE");
        Room room = fixtures.room(acc.getAccommodationId(), 1, 4, "ACTIVE");
        LocalDate checkIn = LocalDate.now().plusDays(20);
        LocalDate checkOut = checkIn.plusDays(1);

        CountDownLatch snapshotTaken = new CountDownLatch(1);
        CountDownLatch firstCommitted = new CountDownLatch(1);
        AtomicReference<Throwable> secondOutcome = new AtomicReference<>();

        // 트랜잭션 B: REPEATABLE READ 로 시작해 일반 SELECT 로 스냅샷을 먼저 고정한 뒤(결제 흐름의 중복 확인 SELECT 와 같은 상황),
        // A 의 커밋을 기다렸다가 같은 객실·날짜로 예약한다.
        TransactionTemplate repeatableRead = new TransactionTemplate(transactionManager);
        repeatableRead.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        CompletableFuture<Void> second = CompletableFuture.runAsync(() -> {
            try {
                repeatableRead.executeWithoutResult(status -> {
                    reservationRepository.existsByReservationNumber("snapshot-probe"); // 첫 일관된 읽기 → 스냅샷 고정
                    snapshotTaken.countDown();
                    try {
                        firstCommitted.await(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    // createReservation(@Transactional REQUIRED)은 이 REPEATABLE READ 트랜잭션에 참여한다.
                    reserveService.createReservation(request(room.getRoomId(), checkIn, checkOut),
                            new BigDecimal("100000.00"), BigDecimal.ZERO);
                });
            } catch (Throwable t) {
                secondOutcome.set(t);
            }
        });

        // 트랜잭션 A: B 의 스냅샷 이후에 예약을 만들고 커밋한다.
        assertThat(snapshotTaken.await(30, TimeUnit.SECONDS)).isTrue();
        reserveService.createReservation(request(room.getRoomId(), checkIn, checkOut),
                new BigDecimal("100000.00"), BigDecimal.ZERO);
        firstCommitted.countDown();
        second.get(60, TimeUnit.SECONDS);

        // 과거(일반 SELECT COUNT)라면 B 는 스냅샷에서 0건을 보고 두 번째 예약을 만들었다(초과 예약).
        assertThat(secondOutcome.get())
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(occupying(room.getRoomId())).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 쿠폰으로 동시에 사용 처리하면 정확히 1건만 성공한다")
    void concurrentCouponUsage() throws Exception {
        User user = fixtures.user(0);
        Accommodation acc = fixtures.accommodation(TestFixtures.unique("쿠폰동시시"), "ACTIVE");
        Room room = fixtures.room(acc.getAccommodationId(), 10, 4, "ACTIVE");
        Coupon coupon = fixtures.coupon(b -> {});
        UserCoupon issued = fixtures.userCoupon(user.getUserId(), coupon.getCouponId(), "ISSUED");
        int threads = 4;
        List<Reservation> reservations = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            reservations.add(fixtures.reservation(user.getUserId(), room.getRoomId(), ReservationStatus.CONFIRMED,
                    LocalDate.now().plusDays(30 + i), LocalDate.now().plusDays(31 + i)));
        }

        ConcurrencyResult result = ConcurrencyResult.run(threads, i -> couponUsageService.recordUsage(
                issued.getUserCouponId(), user.getUserId(), reservations.get(i), new BigDecimal("5000")));

        assertThat(result.successes()).isEqualTo(1);
        assertThat(result.failures()).hasSize(threads - 1).allSatisfy(e -> assertThat(e).isInstanceOf(ApiException.class));
        Long usages = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM coupon_usage WHERE user_coupon_id = ?", Long.class, issued.getUserCouponId());
        assertThat(usages).isEqualTo(1L);
    }

    @Test
    @DisplayName("같은 잔액(5,000P)으로 4,000P 를 동시에 두 번 쓰면 하나만 성공하고 잔액은 1,000P")
    void concurrentPointUse() throws Exception {
        User user = fixtures.user(5000);

        ConcurrencyResult result = ConcurrencyResult.run(2, i -> transactionTemplate.executeWithoutResult(s ->
                pointService.use(entityManager.find(User.class, user.getUserId()), 4000, "예약 시 포인트 사용", 900L + i)));

        assertThat(result.successes()).isEqualTo(1);
        assertThat(result.failures()).singleElement().isInstanceOf(ApiException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT point FROM user WHERE user_id = ?", Integer.class, user.getUserId()))
                .isEqualTo(1000);
    }
}
