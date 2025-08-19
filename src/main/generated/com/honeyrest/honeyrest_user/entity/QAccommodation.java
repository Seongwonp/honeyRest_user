package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAccommodation is a Querydsl query type for Accommodation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAccommodation extends EntityPathBase<Accommodation> {

    private static final long serialVersionUID = -1144198546L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAccommodation accommodation = new QAccommodation("accommodation");

    public final NumberPath<Long> accommodationId = createNumber("accommodationId", Long.class);

    public final StringPath address = createString("address");

    public final StringPath amenities = createString("amenities");

    public final QAccommodationCategory category;

    public final DateTimePath<java.time.LocalDateTime> checkInTime = createDateTime("checkInTime", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> checkOutTime = createDateTime("checkOutTime", java.time.LocalDateTime.class);

    public final QCompany company;

    public final StringPath description = createString("description");

    public final NumberPath<java.math.BigDecimal> latitude = createNumber("latitude", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> longitude = createNumber("longitude", java.math.BigDecimal.class);

    public final QRegion mainRegion;

    public final NumberPath<java.math.BigDecimal> minPrice = createNumber("minPrice", java.math.BigDecimal.class);

    public final StringPath name = createString("name");

    public final NumberPath<java.math.BigDecimal> rating = createNumber("rating", java.math.BigDecimal.class);

    public final StringPath status = createString("status");

    public final QRegion subRegion;

    public final StringPath thumbnail = createString("thumbnail");

    public QAccommodation(String variable) {
        this(Accommodation.class, forVariable(variable), INITS);
    }

    public QAccommodation(Path<? extends Accommodation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAccommodation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAccommodation(PathMetadata metadata, PathInits inits) {
        this(Accommodation.class, metadata, inits);
    }

    public QAccommodation(Class<? extends Accommodation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.category = inits.isInitialized("category") ? new QAccommodationCategory(forProperty("category")) : null;
        this.company = inits.isInitialized("company") ? new QCompany(forProperty("company")) : null;
        this.mainRegion = inits.isInitialized("mainRegion") ? new QRegion(forProperty("mainRegion")) : null;
        this.subRegion = inits.isInitialized("subRegion") ? new QRegion(forProperty("subRegion")) : null;
    }

}

