package com.honeyrest.domain.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "accommodation")
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accommodation_id", nullable = false)
    private Long accommodationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private AccommodationCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_region_id", nullable = false)
    private Region mainRegion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_region_id", nullable = false)
    private Region subRegion;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(length = 500)
    private String thumbnail;

    @Column(columnDefinition = "TEXT")
    private String description;

    // JSON 텍스트를 담는 문자열 컬럼. 실제 스키마(Flyway V1)는 LONGTEXT 이므로 매핑도 LONGTEXT 로 맞춘다.
    // 과거 columnDefinition = "JSON" 이면 H2(demo/e2e/test, ddl-auto=create-drop)가 JSON 타입 컬럼을 만들고,
    // 문자열 파라미터를 JSON '문자열 값'으로 감싸 저장해 저장할 때마다 따옴표가 누적됐다('["a"]' → "\"[...]\"").
    // MySQL 은 Flyway 가 스키마를 관리하므로(ddl-auto=validate) 이 값은 H2 DDL 에만 영향을 준다.
    @Column(columnDefinition = "LONGTEXT")
    private String amenities; // 편의시설 정보(JSON 문자열 저장)

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating; // 평균 평점

    @Column(name = "min_price", precision = 10, scale = 2)
    private BigDecimal minPrice; // 최저 가격

    @Column(length = 20)
    private String status; // 운영 상태(active, inactive)


    public void updateRating(BigDecimal rating) {
        if (rating == null || rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(new BigDecimal("5.0")) > 0) {
            throw new IllegalArgumentException("평점은 0.0 ~ 5.0 사이여야 합니다.");
        }
        this.rating = rating;
    }
}
