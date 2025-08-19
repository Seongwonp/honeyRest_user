package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAccommodationCategory is a Querydsl query type for AccommodationCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAccommodationCategory extends EntityPathBase<AccommodationCategory> {

    private static final long serialVersionUID = 2038939020L;

    public static final QAccommodationCategory accommodationCategory = new QAccommodationCategory("accommodationCategory");

    public final NumberPath<Integer> categoryId = createNumber("categoryId", Integer.class);

    public final StringPath iconUrl = createString("iconUrl");

    public final StringPath name = createString("name");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    public QAccommodationCategory(String variable) {
        super(AccommodationCategory.class, forVariable(variable));
    }

    public QAccommodationCategory(Path<? extends AccommodationCategory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAccommodationCategory(PathMetadata metadata) {
        super(AccommodationCategory.class, metadata);
    }

}

