package com.honeyrest.domain.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "company")
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Integer companyId; // 업체 고유 식별자

    @Column(name = "name", nullable = false, length = 200)
    private String name; // 업체명

    @Column(name = "business_number", nullable = false, length = 50)
    private String businessNumber; // 사업자 등록번호

    @Column(name = "owner_name",length = 20)
    private String ownerName; // 대표자명

    @Column(name = "phone", length = 20)
    private String phone; // 대표 연락처

    @Column(name = "email", length = 100)
    private String email; // 대표 이메일

    @Column(name = "address", length = 255)
    private String address; // 사업장 주소

    // JSON 텍스트를 담는 문자열 컬럼. 실제 스키마(Flyway V1)는 LONGTEXT 이므로 매핑도 LONGTEXT 로 맞춘다.
    // 과거 columnDefinition = "JSON" 이면 H2(demo/e2e/test, ddl-auto=create-drop)가 JSON 타입 컬럼을 만들고,
    // 문자열 파라미터를 JSON '문자열 값'으로 감싸 저장해 저장할 때마다 따옴표가 누적됐다('["a"]' → "\"[...]\"").
    // MySQL 은 Flyway 가 스키마를 관리하므로(ddl-auto=validate) 이 값은 H2 DDL 에만 영향을 준다.
    @Column(name = "bank_info", columnDefinition = "LONGTEXT")
    private String bankInfo; // 은행명

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate; // 수수료율

    @Column(name = "status", length = 20)
    private String status; // 승인 상태

}
