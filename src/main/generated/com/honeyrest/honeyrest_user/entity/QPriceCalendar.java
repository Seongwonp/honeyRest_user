package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPriceCalendar is a Querydsl query type for PriceCalendar
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPriceCalendar extends EntityPathBase<PriceCalendar> {

    private static final long serialVersionUID = -1359076925L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPriceCalendar priceCalendar = new QPriceCalendar("priceCalendar");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final NumberPath<Integer> availableRoom = createNumber("availableRoom", Integer.class);

    public final NumberPath<Long> calendarId = createNumber("calendarId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DatePath<java.time.LocalDate> date = createDate("date", java.time.LocalDate.class);

    public final NumberPath<java.math.BigDecimal> price = createNumber("price", java.math.BigDecimal.class);

    public final QRoom room;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QPriceCalendar(String variable) {
        this(PriceCalendar.class, forVariable(variable), INITS);
    }

    public QPriceCalendar(Path<? extends PriceCalendar> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPriceCalendar(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPriceCalendar(PathMetadata metadata, PathInits inits) {
        this(PriceCalendar.class, metadata, inits);
    }

    public QPriceCalendar(Class<? extends PriceCalendar> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.room = inits.isInitialized("room") ? new QRoom(forProperty("room"), inits.get("room")) : null;
    }

}

