-- HoneyRest baseline schema
-- This script is the baseline for existing databases.
-- If the DB already exists with tables, Flyway will mark this as applied via baseline-on-migrate.

CREATE TABLE IF NOT EXISTS `accommodation_category` (
    `category_id` INT NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(100) NOT NULL,
    `icon_url`    VARCHAR(255),
    `sort_order`  INT,
    PRIMARY KEY (`category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `region` (
    `region_id`  INT NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(100) NOT NULL,
    `level`      INT NOT NULL,
    `parent_id`  INT,
    `image_url`  VARCHAR(255),
    `is_popular` BIT(1),
    PRIMARY KEY (`region_id`),
    FOREIGN KEY (`parent_id`) REFERENCES `region` (`region_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `company` (
    `company_id`      INT NOT NULL AUTO_INCREMENT,
    `created_at`      DATETIME(6),
    `updated_at`      DATETIME(6),
    `name`            VARCHAR(200) NOT NULL,
    `business_number` VARCHAR(50)  NOT NULL,
    `owner_name`      VARCHAR(20),
    `phone`           VARCHAR(20),
    `email`           VARCHAR(100),
    `address`         VARCHAR(255),
    `bank_info`       LONGTEXT,
    `commission_rate` DECIMAL(5, 2),
    `status`          VARCHAR(20),
    PRIMARY KEY (`company_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
    `user_id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `created_at`      DATETIME(6),
    `updated_at`      DATETIME(6),
    `email`           VARCHAR(100) NOT NULL UNIQUE,
    `password_hash`   VARCHAR(255),
    `social_type`     VARCHAR(20),
    `social_id`       VARCHAR(100),
    `name`            VARCHAR(50)  NOT NULL,
    `phone`           VARCHAR(20),
    `profile_image`   VARCHAR(500),
    `birth_date`      DATE,
    `gender`          VARCHAR(10),
    `marketing_agree` BIT(1),
    `point`           INT NOT NULL DEFAULT 0,
    `role`            VARCHAR(20),
    `status`          VARCHAR(20),
    `last_login`      DATETIME(6),
    `is_verified`     BIT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `accommodation` (
    `accommodation_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `company_id`       INT          NOT NULL,
    `category_id`      INT          NOT NULL,
    `main_region_id`   INT          NOT NULL,
    `sub_region_id`    INT          NOT NULL,
    `name`             VARCHAR(255) NOT NULL,
    `address`          VARCHAR(500) NOT NULL,
    `latitude`         DECIMAL(10, 6),
    `longitude`        DECIMAL(10, 6),
    `thumbnail`        VARCHAR(500),
    `description`      TEXT,
    `amenities`        LONGTEXT,
    `check_in_time`    DATETIME(6),
    `check_out_time`   DATETIME(6),
    `rating`           DECIMAL(2, 1),
    `min_price`        DECIMAL(10, 2),
    `status`           VARCHAR(20),
    PRIMARY KEY (`accommodation_id`),
    FOREIGN KEY (`company_id`)     REFERENCES `company` (`company_id`),
    FOREIGN KEY (`category_id`)    REFERENCES `accommodation_category` (`category_id`),
    FOREIGN KEY (`main_region_id`) REFERENCES `region` (`region_id`),
    FOREIGN KEY (`sub_region_id`)  REFERENCES `region` (`region_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `accommodation_image` (
    `accommodation_image_id` BIGINT NOT NULL AUTO_INCREMENT,
    `created_at`             DATETIME(6),
    `updated_at`             DATETIME(6),
    `image_url`              VARCHAR(500) NOT NULL,
    `accommodation_id`       BIGINT NOT NULL,
    PRIMARY KEY (`accommodation_image_id`),
    FOREIGN KEY (`accommodation_id`) REFERENCES `accommodation` (`accommodation_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `accommodation_tag` (
    `tag_id`    BIGINT       NOT NULL AUTO_INCREMENT,
    `name`      VARCHAR(100) NOT NULL,
    `icon_name` VARCHAR(50),
    PRIMARY KEY (`tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `accommodation_tag_map` (
    `tag_map_id`       BIGINT NOT NULL AUTO_INCREMENT,
    `accommodation_id` BIGINT NOT NULL,
    `tag_id`           BIGINT NOT NULL,
    PRIMARY KEY (`tag_map_id`),
    FOREIGN KEY (`accommodation_id`) REFERENCES `accommodation` (`accommodation_id`),
    FOREIGN KEY (`tag_id`)           REFERENCES `accommodation_tag` (`tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `room` (
    `room_id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `created_at`         DATETIME(6),
    `updated_at`         DATETIME(6),
    `accommodation_id`   BIGINT       NOT NULL,
    `name`               VARCHAR(150) NOT NULL,
    `type`               VARCHAR(60),
    `price`              DECIMAL(10, 2) NOT NULL,
    `max_occupancy`      INT          NOT NULL,
    `standard_occupancy` INT          NOT NULL,
    `extra_person_fee`   DECIMAL(10, 2),
    `bed_info`           LONGTEXT,
    `amenities`          LONGTEXT,
    `description`        TEXT,
    `total_rooms`        INT          NOT NULL,
    `status`             VARCHAR(20),
    PRIMARY KEY (`room_id`),
    FOREIGN KEY (`accommodation_id`) REFERENCES `accommodation` (`accommodation_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `room_image` (
    `room_image_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `created_at`    DATETIME(6),
    `updated_at`    DATETIME(6),
    `image_url`     VARCHAR(500) NOT NULL,
    `room_id`       BIGINT       NOT NULL,
    PRIMARY KEY (`room_image_id`),
    FOREIGN KEY (`room_id`) REFERENCES `room` (`room_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `reservation` (
    `reservation_id`     BIGINT       NOT NULL AUTO_INCREMENT,
    `created_at`         DATETIME(6),
    `updated_at`         DATETIME(6),
    `user_id`            BIGINT,
    `room_id`            BIGINT       NOT NULL,
    `accommodation_id`   BIGINT       NOT NULL,
    `room_name`          VARCHAR(255) NOT NULL,
    `reservation_number` VARCHAR(50)  NOT NULL UNIQUE,
    `check_in_date`      DATE         NOT NULL,
    `check_out_date`     DATE         NOT NULL,
    `guest_count`        INT          NOT NULL,
    `guest_name`         VARCHAR(100) NOT NULL,
    `guest_phone`        VARCHAR(20)  NOT NULL,
    `price`              DECIMAL(10, 2) NOT NULL,
    `original_price`     DECIMAL(10, 2),
    `discount_amount`    DECIMAL(10, 2),
    `status`             VARCHAR(20)  NOT NULL,
    `cancel_reason`      TEXT,
    `special_requests`   TEXT,
    PRIMARY KEY (`reservation_id`),
    FOREIGN KEY (`user_id`)          REFERENCES `user` (`user_id`),
    FOREIGN KEY (`room_id`)          REFERENCES `room` (`room_id`),
    FOREIGN KEY (`accommodation_id`) REFERENCES `accommodation` (`accommodation_id`),
    INDEX `idx_reservation_user_id`       (`user_id`),
    INDEX `idx_reservation_status`        (`status`),
    INDEX `idx_reservation_check_in_date` (`check_in_date`),
    INDEX `idx_reservation_check_out_date`(`check_out_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `payment` (
    `payment_id`     BIGINT      NOT NULL AUTO_INCREMENT,
    `created_at`     DATETIME(6),
    `updated_at`     DATETIME(6),
    `reservation_id` BIGINT      NOT NULL,
    `user_id`        BIGINT,
    `amount`         DECIMAL(10, 2) NOT NULL,
    `payment_method` VARCHAR(30) NOT NULL,
    `payment_status` VARCHAR(20) NOT NULL,
    `payment_date`   DATETIME(6),
    `pg_provider`    VARCHAR(50),
    `transaction_id` VARCHAR(100),
    `receipt_url`    VARCHAR(500),
    PRIMARY KEY (`payment_id`),
    FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`reservation_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `payment_detail` (
    `payment_detail_id`       BIGINT NOT NULL AUTO_INCREMENT,
    `created_at`              DATETIME(6),
    `updated_at`              DATETIME(6),
    `payment_id`              BIGINT NOT NULL,
    `toss_payment_key`        VARCHAR(100),
    `card_company`            VARCHAR(50),
    `card_number`             VARCHAR(50),
    `installment_months`      INT,
    `virtual_account_bank`    VARCHAR(50),
    `virtual_account_number`  VARCHAR(50),
    `virtual_account_holder`  VARCHAR(50),
    `virtual_account_expire`  DATETIME(6),
    PRIMARY KEY (`payment_detail_id`),
    FOREIGN KEY (`payment_id`) REFERENCES `payment` (`payment_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `review` (
    `review_id`          BIGINT NOT NULL AUTO_INCREMENT,
    `created_at`         DATETIME(6),
    `updated_at`         DATETIME(6),
    `reservation_id`     BIGINT NOT NULL,
    `user_id`            BIGINT NOT NULL,
    `accommodation_id`   BIGINT NOT NULL,
    `room_id`            BIGINT NOT NULL,
    `rating`             DECIMAL(3, 2) NOT NULL,
    `cleanliness_rating` DECIMAL(3, 2),
    `service_rating`     DECIMAL(3, 2),
    `facilities_rating`  DECIMAL(3, 2),
    `location_rating`    DECIMAL(3, 2),
    `content`            TEXT,
    `reply`              TEXT,
    `like_count`         INT,
    `status`             VARCHAR(20) NOT NULL,
    PRIMARY KEY (`review_id`),
    FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`reservation_id`),
    FOREIGN KEY (`user_id`)        REFERENCES `user` (`user_id`),
    INDEX `idx_review_user_id`          (`user_id`),
    INDEX `idx_review_reservation_id`   (`reservation_id`),
    INDEX `idx_review_accommodation_id` (`accommodation_id`),
    INDEX `idx_review_status`           (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `review_image` (
    `review_image_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `created_at`      DATETIME(6),
    `updated_at`      DATETIME(6),
    `review_id`       BIGINT       NOT NULL,
    `image_url`       VARCHAR(500) NOT NULL,
    PRIMARY KEY (`review_image_id`),
    FOREIGN KEY (`review_id`) REFERENCES `review` (`review_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `coupon` (
    `coupon_id`           BIGINT      NOT NULL AUTO_INCREMENT,
    `created_at`          DATETIME(6),
    `updated_at`          DATETIME(6),
    `code`                VARCHAR(50) UNIQUE,
    `name`                VARCHAR(50) NOT NULL,
    `discount_type`       VARCHAR(255) NOT NULL,
    `discount_value`      DECIMAL(10, 2) NOT NULL,
    `min_order_amount`    DECIMAL(10, 2),
    `max_discount_amount` DECIMAL(10, 2),
    `start_date`          DATETIME(6) NOT NULL,
    `end_date`            DATETIME(6) NOT NULL,
    `is_active`           BIT(1) NOT NULL DEFAULT 1,
    `target_type`         VARCHAR(20) NOT NULL,
    `target_id`           BIGINT,
    PRIMARY KEY (`coupon_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `user_coupon` (
    `user_coupon_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT NOT NULL,
    `coupon_id`      BIGINT NOT NULL,
    `status`         VARCHAR(20) NOT NULL,
    `issued_at`      DATETIME(6) NOT NULL,
    `expired_at`     DATETIME(6) NOT NULL,
    `used_at`        DATETIME(6),
    PRIMARY KEY (`user_coupon_id`),
    FOREIGN KEY (`user_id`)   REFERENCES `user` (`user_id`),
    FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`coupon_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `coupon_usage` (
    `coupon_usage_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_coupon_id`  BIGINT NOT NULL,
    `reservation_id`  BIGINT NOT NULL,
    `discount_amount` DECIMAL(10, 2),
    `used_at`         DATETIME(6),
    PRIMARY KEY (`coupon_usage_id`),
    FOREIGN KEY (`user_coupon_id`) REFERENCES `user_coupon` (`user_coupon_id`),
    FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`reservation_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `banner` (
    `banner_id`  BIGINT       NOT NULL AUTO_INCREMENT,
    `created_at` DATETIME(6),
    `updated_at` DATETIME(6),
    `title`      VARCHAR(200) NOT NULL,
    `image_url`  VARCHAR(500) NOT NULL,
    `target_url` VARCHAR(500),
    `position`   VARCHAR(30),
    `sort_order` INT,
    `start_date` DATETIME(6),
    `end_date`   DATETIME(6),
    `is_active`  BIT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`banner_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `event` (
    `event_id`   BIGINT       NOT NULL AUTO_INCREMENT,
    `created_at` DATETIME(6),
    `updated_at` DATETIME(6),
    `title`      VARCHAR(200) NOT NULL,
    `content`    TEXT,
    `image_url`  VARCHAR(500),
    `start_date` DATETIME(6),
    `end_date`   DATETIME(6),
    `is_active`  BIT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`event_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `refresh_token` (
    `token_id`   BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL,
    `token`      VARCHAR(255) NOT NULL UNIQUE,
    `expires_at` DATETIME(6)  NOT NULL,
    `created_at` DATETIME(6),
    PRIMARY KEY (`token_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `email_verification_token` (
    `token_id`   BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL,
    `token`      VARCHAR(255) NOT NULL UNIQUE,
    `expires_at` DATETIME(6)  NOT NULL,
    `used`       BIT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME(6),
    PRIMARY KEY (`token_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `password_reset_token` (
    `token_id`   BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL,
    `token`      VARCHAR(255) NOT NULL UNIQUE,
    `expires_at` DATETIME(6)  NOT NULL,
    `used`       BIT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME(6),
    PRIMARY KEY (`token_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `point_history` (
    `point_history_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT NOT NULL,
    `amount`           INT NOT NULL,
    `type`             VARCHAR(20) NOT NULL,
    `reason`           VARCHAR(200),
    `related_id`       BIGINT,
    `created_at`       DATETIME(6),
    `expires_at`       DATETIME(6),
    PRIMARY KEY (`point_history_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `wish_list` (
    `wish_list_id`     BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT NOT NULL,
    `accommodation_id` BIGINT NOT NULL,
    `created_at`       DATETIME(6),
    PRIMARY KEY (`wish_list_id`),
    FOREIGN KEY (`user_id`)          REFERENCES `user` (`user_id`),
    FOREIGN KEY (`accommodation_id`) REFERENCES `accommodation` (`accommodation_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `inquiry` (
    `inquiry_id`  BIGINT NOT NULL AUTO_INCREMENT,
    `created_at`  DATETIME(6),
    `updated_at`  DATETIME(6),
    `user_id`     BIGINT NOT NULL,
    `title`       VARCHAR(200) NOT NULL,
    `content`     TEXT NOT NULL,
    `status`      VARCHAR(20) NOT NULL,
    `answer`      TEXT,
    `answered_at` DATETIME(6),
    PRIMARY KEY (`inquiry_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `notification` (
    `notification_id` BIGINT NOT NULL AUTO_INCREMENT,
    `created_at`      DATETIME(6),
    `updated_at`      DATETIME(6),
    `user_id`         BIGINT NOT NULL,
    `title`           VARCHAR(200),
    `message`         TEXT,
    `type`            VARCHAR(50),
    `is_read`         BIT(1) NOT NULL DEFAULT 0,
    `related_id`      BIGINT,
    PRIMARY KEY (`notification_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `cancellation_policy` (
    `policy_id`        BIGINT NOT NULL AUTO_INCREMENT,
    `accommodation_id` BIGINT NOT NULL,
    `days_before`      INT NOT NULL,
    `refund_rate`      DECIMAL(5, 2) NOT NULL,
    `description`      VARCHAR(500),
    PRIMARY KEY (`policy_id`),
    FOREIGN KEY (`accommodation_id`) REFERENCES `accommodation` (`accommodation_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `price_calendar` (
    `price_calendar_id` BIGINT         NOT NULL AUTO_INCREMENT,
    `room_id`           BIGINT         NOT NULL,
    `date`              DATE           NOT NULL,
    `price`             DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (`price_calendar_id`),
    UNIQUE KEY `uq_room_date` (`room_id`, `date`),
    FOREIGN KEY (`room_id`) REFERENCES `room` (`room_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `accommodation_request_map` (
    `request_map_id`   BIGINT NOT NULL AUTO_INCREMENT,
    `accommodation_id` BIGINT NOT NULL,
    `request_content`  TEXT,
    PRIMARY KEY (`request_map_id`),
    FOREIGN KEY (`accommodation_id`) REFERENCES `accommodation` (`accommodation_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
