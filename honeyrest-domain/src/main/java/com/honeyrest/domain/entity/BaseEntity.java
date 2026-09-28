package com.honeyrest.domain.entity;


import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * created_at / updated_at 공통 컬럼.
 * <p>
 * [통합 전] 사용자 API 는 Spring Data {@code AuditingEntityListener}(@CreatedDate/@LastModifiedDate),
 * 호스트는 같은 리스너 + {@code @PrePersist/@PreUpdate} 폴백을 썼다.
 * 공유 도메인 모듈은 Spring 에 의존하지 않도록 Hibernate 의 {@code @CreationTimestamp}/{@code @UpdateTimestamp} 로 통일한다.
 * INSERT 시 두 값이 모두 채워지고 UPDATE 시 updated_at 만 갱신되므로 기존 두 방식과 결과가 같다.
 * (앱의 {@code @EnableJpaAuditing} 은 남아 있어도 무해하다.)
 */
@Getter
@MappedSuperclass
public class BaseEntity {

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
