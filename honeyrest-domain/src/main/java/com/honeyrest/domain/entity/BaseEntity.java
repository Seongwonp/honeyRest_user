package com.honeyrest.domain.entity;


import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * created_at / updated_at 공통 컬럼.
 * 도메인 모듈이 Spring 에 의존하지 않도록 Hibernate 의 {@code @CreationTimestamp}/{@code @UpdateTimestamp} 를 쓴다.
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
