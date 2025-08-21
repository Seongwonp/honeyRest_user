package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAccommodationTag is a Querydsl query type for AccommodationTag
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAccommodationTag extends EntityPathBase<AccommodationTag> {

    private static final long serialVersionUID = -1958338996L;

    public static final QAccommodationTag accommodationTag = new QAccommodationTag("accommodationTag");

    public final StringPath category = createString("category");

    public final StringPath iconName = createString("iconName");

    public final StringPath name = createString("name");

    public final NumberPath<Long> tagId = createNumber("tagId", Long.class);

    public QAccommodationTag(String variable) {
        super(AccommodationTag.class, forVariable(variable));
    }

    public QAccommodationTag(Path<? extends AccommodationTag> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAccommodationTag(PathMetadata metadata) {
        super(AccommodationTag.class, metadata);
    }

}

