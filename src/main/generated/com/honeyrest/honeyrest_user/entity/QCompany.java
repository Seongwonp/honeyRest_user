package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCompany is a Querydsl query type for Company
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCompany extends EntityPathBase<Company> {

    private static final long serialVersionUID = 1526360985L;

    public static final QCompany company = new QCompany("company");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final StringPath address = createString("address");

    public final StringPath bankInfo = createString("bankInfo");

    public final StringPath businessNumber = createString("businessNumber");

    public final NumberPath<java.math.BigDecimal> commissionRate = createNumber("commissionRate", java.math.BigDecimal.class);

    public final NumberPath<Integer> companyId = createNumber("companyId", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath email = createString("email");

    public final StringPath name = createString("name");

    public final StringPath ownerName = createString("ownerName");

    public final StringPath phone = createString("phone");

    public final StringPath status = createString("status");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QCompany(String variable) {
        super(Company.class, forVariable(variable));
    }

    public QCompany(Path<? extends Company> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCompany(PathMetadata metadata) {
        super(Company.class, metadata);
    }

}

