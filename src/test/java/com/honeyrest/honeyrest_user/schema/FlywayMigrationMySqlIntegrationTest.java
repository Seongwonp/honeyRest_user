package com.honeyrest.honeyrest_user.schema;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 MySQL 8.0 (Testcontainers) 위에서 공유 스키마 마이그레이션을 검증한다.
 *
 * <p>기본 {@code test} 태스크는 H2 + create-drop 이라 Flyway SQL 자체(MySQL 전용 PREPARE/information_schema 구문 포함)와
 * "마이그레이션 결과 스키마 == 엔티티 매핑" 여부를 검증하지 못한다. 이 테스트는 빈 DB 에서
 * <ol>
 *   <li>Flyway V1~최신 마이그레이션이 모두 성공하는지</li>
 *   <li>{@code reservation.accommodation_name} 이 NOT NULL 인지 (V10, 호스트 매핑과 동일)</li>
 *   <li>그 스키마에 대해 사용자 엔티티의 Hibernate {@code ddl-auto=validate} 가 통과하는지 (컨텍스트 기동 자체가 검증)</li>
 * </ol>
 * 를 확인한다. Docker 가 없으면 클래스 전체를 건너뛴다({@code disabledWithoutDocker}).
 * 실행: {@code ./gradlew integrationTest}
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test") // 더미 비밀값/로컬 저장소/simple 캐시는 test 프로필을 재사용하고 DB 관련 설정만 아래에서 덮어쓴다.
class FlywayMigrationMySqlIntegrationTest {

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
        // 운영과 같은 조합: Flyway 가 스키마를 만들고 JPA 는 검증만 한다.
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("app.storage.type", () -> "local");
    }

    @Autowired
    Flyway flyway;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DataSource dataSource;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void 빈_DB에서_모든_마이그레이션이_성공한다() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(flyway.info().pending()).as("미적용 마이그레이션").isEmpty();
        assertThat(applied).as("적용된 마이그레이션").isNotEmpty()
                .allSatisfy(info -> assertThat(info.getState())
                        .as("V%s 상태", info.getVersion())
                        .isEqualTo(MigrationState.SUCCESS));

        List<String> versions = Arrays.stream(applied)
                .map(info -> info.getVersion().getVersion())
                .toList();
        assertThat(versions).contains("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
    }

    @Test
    void reservation_accommodation_name_은_NOT_NULL_이다() {
        String isNullable = columnAttribute("reservation", "accommodation_name", "is_nullable");
        String columnType = columnAttribute("reservation", "accommodation_name", "column_type");

        assertThat(isNullable).isEqualTo("NO");
        assertThat(columnType).isEqualToIgnoringCase("varchar(255)");
    }

    @Test
    void 사용자_엔티티가_마이그레이션_스키마에_대해_validate_를_통과한다() {
        // validate 가 실패했다면 컨텍스트 기동 단계에서 SchemaManagementException 으로 이미 실패한다.
        // 여기서는 정말로 validate 모드로 기동했는지(다른 설정으로 우회되지 않았는지)를 확인한다.
        assertThat(entityManagerFactory.getProperties())
                .containsEntry("hibernate.hbm2ddl.auto", "validate");
    }

    @Test
    void V11_호스트_정합_항목이_반영되어_있다() {
        Integer errorLogTables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'error_log'",
                Integer.class);
        assertThat(errorLogTables).as("호스트 ErrorLog 엔티티용 error_log 테이블").isEqualTo(1);

        assertThat(columnAttribute("cancellation_policy", "days_before", "is_nullable")).isEqualTo("YES");
        assertThat(columnAttribute("cancellation_policy", "refund_rate", "is_nullable")).isEqualTo("YES");
    }

    @Test
    void V10_V11_은_이미_적용된_DB에서_다시_실행해도_안전하다() {
        // 개발 DB 처럼 과거 ddl-auto=update 로 컬럼/테이블이 먼저 생긴 상태를 흉내 내기 위해 같은 SQL 을 한 번 더 실행한다.
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V10__add_reservation_accommodation_name.sql"),
                new ClassPathResource("db/migration/V11__host_schema_alignment.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);

        assertThat(columnAttribute("reservation", "accommodation_name", "is_nullable")).isEqualTo("NO");
        assertThat(columnAttribute("cancellation_policy", "days_before", "is_nullable")).isEqualTo("YES");
    }

    private String columnAttribute(String table, String column, String attribute) {
        return jdbcTemplate.queryForObject(
                "SELECT " + attribute + " FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                String.class, table, column);
    }
}
