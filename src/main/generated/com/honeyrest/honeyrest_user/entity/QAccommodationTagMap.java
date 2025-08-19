package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAccommodationTagMap is a Querydsl query type for AccommodationTagMap
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAccommodationTagMap extends EntityPathBase<AccommodationTagMap> {

    private static final long serialVersionUID = 1958796144L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAccommodationTagMap accommodationTagMap = new QAccommodationTagMap("accommodationTagMap");

    public final QAccommodation accommodation;

    public final NumberPath<Long> mapId = createNumber("mapId", Long.class);

    public final QAccommodationTag tag;

    public QAccommodationTagMap(String variable) {
        this(AccommodationTagMap.class, forVariable(variable), INITS);
    }

    public QAccommodationTagMap(Path<? extends AccommodationTagMap> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAccommodationTagMap(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAccommodationTagMap(PathMetadata metadata, PathInits inits) {
        this(AccommodationTagMap.class, metadata, inits);
    }

    public QAccommodationTagMap(Class<? extends AccommodationTagMap> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.accommodation = inits.isInitialized("accommodation") ? new QAccommodation(forProperty("accommodation"), inits.get("accommodation")) : null;
        this.tag = inits.isInitialized("tag") ? new QAccommodationTag(forProperty("tag")) : null;
    }

}

