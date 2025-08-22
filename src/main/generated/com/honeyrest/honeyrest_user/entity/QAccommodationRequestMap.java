package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAccommodationRequestMap is a Querydsl query type for AccommodationRequestMap
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAccommodationRequestMap extends EntityPathBase<AccommodationRequestMap> {

    private static final long serialVersionUID = -344125093L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAccommodationRequestMap accommodationRequestMap = new QAccommodationRequestMap("accommodationRequestMap");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final QAccommodation accommodation;

    public final QCompany company;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final StringPath fileUrl = createString("fileUrl");

    public final NumberPath<Long> RequestMapId = createNumber("RequestMapId", Long.class);

    public final StringPath status = createString("status");

    public final QAccommodationTagMap tagMap;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QAccommodationRequestMap(String variable) {
        this(AccommodationRequestMap.class, forVariable(variable), INITS);
    }

    public QAccommodationRequestMap(Path<? extends AccommodationRequestMap> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAccommodationRequestMap(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAccommodationRequestMap(PathMetadata metadata, PathInits inits) {
        this(AccommodationRequestMap.class, metadata, inits);
    }

    public QAccommodationRequestMap(Class<? extends AccommodationRequestMap> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.accommodation = inits.isInitialized("accommodation") ? new QAccommodation(forProperty("accommodation"), inits.get("accommodation")) : null;
        this.company = inits.isInitialized("company") ? new QCompany(forProperty("company")) : null;
        this.tagMap = inits.isInitialized("tagMap") ? new QAccommodationTagMap(forProperty("tagMap"), inits.get("tagMap")) : null;
    }

}

