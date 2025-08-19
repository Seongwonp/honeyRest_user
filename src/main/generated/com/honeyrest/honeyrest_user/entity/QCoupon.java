package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCoupon is a Querydsl query type for Coupon
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCoupon extends EntityPathBase<Coupon> {

    private static final long serialVersionUID = 880760202L;

    public static final QCoupon coupon = new QCoupon("coupon");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final StringPath code = createString("code");

    public final NumberPath<Long> couponId = createNumber("couponId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath discountType = createString("discountType");

    public final NumberPath<java.math.BigDecimal> discountValue = createNumber("discountValue", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> endDate = createDateTime("endDate", java.time.LocalDateTime.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final NumberPath<java.math.BigDecimal> maxOrderAmount = createNumber("maxOrderAmount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> minOrderAmount = createNumber("minOrderAmount", java.math.BigDecimal.class);

    public final StringPath name = createString("name");

    public final DateTimePath<java.time.LocalDateTime> startDate = createDateTime("startDate", java.time.LocalDateTime.class);

    public final NumberPath<Long> targetId = createNumber("targetId", Long.class);

    public final StringPath targetType = createString("targetType");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QCoupon(String variable) {
        super(Coupon.class, forVariable(variable));
    }

    public QCoupon(Path<? extends Coupon> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCoupon(PathMetadata metadata) {
        super(Coupon.class, metadata);
    }

}

