package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBanner is a Querydsl query type for Banner
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBanner extends EntityPathBase<Banner> {

    private static final long serialVersionUID = 838990992L;

    public static final QBanner banner = new QBanner("banner");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final NumberPath<Long> bannerId = createNumber("bannerId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> endDate = createDateTime("endDate", java.time.LocalDateTime.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final BooleanPath isActive = createBoolean("isActive");

    public final EnumPath<BannerPosition> position = createEnum("position", BannerPosition.class);

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> startDate = createDateTime("startDate", java.time.LocalDateTime.class);

    public final StringPath targetUrl = createString("targetUrl");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QBanner(String variable) {
        super(Banner.class, forVariable(variable));
    }

    public QBanner(Path<? extends Banner> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBanner(PathMetadata metadata) {
        super(Banner.class, metadata);
    }

}

