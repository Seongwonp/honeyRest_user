package com.honeyrest.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cancellation_policy")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id")
    private Accommodation accommodation;

    private String policyName;

    @Column(columnDefinition = "TEXT")
    private String detail; // 환불 규정 상세(JSON 문자열, 스키마는 TEXT)
}