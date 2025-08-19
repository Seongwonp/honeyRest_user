package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPaymentDetail is a Querydsl query type for PaymentDetail
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPaymentDetail extends EntityPathBase<PaymentDetail> {

    private static final long serialVersionUID = 554009619L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPaymentDetail paymentDetail = new QPaymentDetail("paymentDetail");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final StringPath cardCompany = createString("cardCompany");

    public final StringPath cardNumber = createString("cardNumber");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> installmentMonths = createNumber("installmentMonths", Integer.class);

    public final QPayment payment;

    public final NumberPath<Long> paymentDetailId = createNumber("paymentDetailId", Long.class);

    public final StringPath tossPaymentKey = createString("tossPaymentKey");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath virtualAccountBank = createString("virtualAccountBank");

    public final DateTimePath<java.time.LocalDateTime> virtualAccountExpire = createDateTime("virtualAccountExpire", java.time.LocalDateTime.class);

    public final StringPath virtualAccountHolder = createString("virtualAccountHolder");

    public final StringPath virtualAccountNumber = createString("virtualAccountNumber");

    public QPaymentDetail(String variable) {
        this(PaymentDetail.class, forVariable(variable), INITS);
    }

    public QPaymentDetail(Path<? extends PaymentDetail> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPaymentDetail(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPaymentDetail(PathMetadata metadata, PathInits inits) {
        this(PaymentDetail.class, metadata, inits);
    }

    public QPaymentDetail(Class<? extends PaymentDetail> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.payment = inits.isInitialized("payment") ? new QPayment(forProperty("payment"), inits.get("payment")) : null;
    }

}

