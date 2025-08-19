package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCouponUsage is a Querydsl query type for CouponUsage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCouponUsage extends EntityPathBase<CouponUsage> {

    private static final long serialVersionUID = -1087496649L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCouponUsage couponUsage = new QCouponUsage("couponUsage");

    public final QBaseEntity _super = new QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<java.math.BigDecimal> discountAmount = createNumber("discountAmount", java.math.BigDecimal.class);

    public final QReservation reservation;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> usageId = createNumber("usageId", Long.class);

    public final QUserCoupon userCoupon;

    public QCouponUsage(String variable) {
        this(CouponUsage.class, forVariable(variable), INITS);
    }

    public QCouponUsage(Path<? extends CouponUsage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCouponUsage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCouponUsage(PathMetadata metadata, PathInits inits) {
        this(CouponUsage.class, metadata, inits);
    }

    public QCouponUsage(Class<? extends CouponUsage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.reservation = inits.isInitialized("reservation") ? new QReservation(forProperty("reservation"), inits.get("reservation")) : null;
        this.userCoupon = inits.isInitialized("userCoupon") ? new QUserCoupon(forProperty("userCoupon"), inits.get("userCoupon")) : null;
    }

}

