package com.honeyrest.honeyrest_user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPasswordResetToken is a Querydsl query type for PasswordResetToken
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPasswordResetToken extends EntityPathBase<PasswordResetToken> {

    private static final long serialVersionUID = 75595337L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPasswordResetToken passwordResetToken = new QPasswordResetToken("passwordResetToken");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> expiryDate = createDateTime("expiryDate", java.time.LocalDateTime.class);

    public final BooleanPath isUsed = createBoolean("isUsed");

    public final NumberPath<Long> tokenId = createNumber("tokenId", Long.class);

    public final StringPath tokenValue = createString("tokenValue");

    public final QUser user;

    public QPasswordResetToken(String variable) {
        this(PasswordResetToken.class, forVariable(variable), INITS);
    }

    public QPasswordResetToken(Path<? extends PasswordResetToken> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPasswordResetToken(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPasswordResetToken(PathMetadata metadata, PathInits inits) {
        this(PasswordResetToken.class, metadata, inits);
    }

    public QPasswordResetToken(Class<? extends PasswordResetToken> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}

