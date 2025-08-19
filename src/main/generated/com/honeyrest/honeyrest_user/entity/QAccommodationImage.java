package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAccommodationImage is a Querydsl query type for AccommodationImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAccommodationImage extends EntityPathBase<AccommodationImage> {

    private static final long serialVersionUID = -777903219L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAccommodationImage accommodationImage = new QAccommodationImage("accommodationImage");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final QAccommodation accommodation;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> imageId = createNumber("imageId", Long.class);

    public final StringPath imageType = createString("imageType");

    public final StringPath imageUrl = createString("imageUrl");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QAccommodationImage(String variable) {
        this(AccommodationImage.class, forVariable(variable), INITS);
    }

    public QAccommodationImage(Path<? extends AccommodationImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAccommodationImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAccommodationImage(PathMetadata metadata, PathInits inits) {
        this(AccommodationImage.class, metadata, inits);
    }

    public QAccommodationImage(Class<? extends AccommodationImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.accommodation = inits.isInitialized("accommodation") ? new QAccommodation(forProperty("accommodation"), inits.get("accommodation")) : null;
    }

}

