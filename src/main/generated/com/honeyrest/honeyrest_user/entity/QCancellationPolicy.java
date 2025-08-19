package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCancellationPolicy is a Querydsl query type for CancellationPolicy
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCancellationPolicy extends EntityPathBase<CancellationPolicy> {

    private static final long serialVersionUID = 755143545L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCancellationPolicy cancellationPolicy = new QCancellationPolicy("cancellationPolicy");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final QAccommodation accommodation;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath detail = createString("detail");

    public final NumberPath<Long> policyId = createNumber("policyId", Long.class);

    public final StringPath policyName = createString("policyName");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QCancellationPolicy(String variable) {
        this(CancellationPolicy.class, forVariable(variable), INITS);
    }

    public QCancellationPolicy(Path<? extends CancellationPolicy> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCancellationPolicy(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCancellationPolicy(PathMetadata metadata, PathInits inits) {
        this(CancellationPolicy.class, metadata, inits);
    }

    public QCancellationPolicy(Class<? extends CancellationPolicy> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.accommodation = inits.isInitialized("accommodation") ? new QAccommodation(forProperty("accommodation"), inits.get("accommodation")) : null;
    }

}

