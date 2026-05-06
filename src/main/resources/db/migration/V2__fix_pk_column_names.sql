-- V2: PK 컬럼명을 JPA 엔티티 @Column(name=) 정의에 맞게 수정
--     + 엔티티에는 있지만 V1에 누락된 컬럼 추가
--
-- [주의] 이 마이그레이션은 V1이 적용된 직후의 상태를 가정합니다.
--       앱이 ddl-auto=update로 이미 일부 컬럼을 추가한 경우(혼재 상태)라면
--       DB를 DROP 후 재생성하여 깨끗하게 시작하세요.

-- ────────────────────────────────────────────────────────────
-- 1. accommodation_image
--    PK: accommodation_image_id → image_id
--    추가: image_type, sort_order
-- ────────────────────────────────────────────────────────────
ALTER TABLE `accommodation_image`
    RENAME COLUMN `accommodation_image_id` TO `image_id`,
    ADD COLUMN `image_type` VARCHAR(50) NULL,
    ADD COLUMN `sort_order` INT NULL;

-- ────────────────────────────────────────────────────────────
-- 2. accommodation_tag_map
--    PK: tag_map_id → map_id
-- ────────────────────────────────────────────────────────────
ALTER TABLE `accommodation_tag_map`
    RENAME COLUMN `tag_map_id` TO `map_id`;

-- ────────────────────────────────────────────────────────────
-- 3. accommodation_request_map
--    PK: request_map_id → requestmap_id
--    컬럼명 변경: request_content → description
--    추가: created_at, updated_at, status, file_url, map_id(FK), company_id(FK)
-- ────────────────────────────────────────────────────────────
ALTER TABLE `accommodation_request_map`
    RENAME COLUMN `request_map_id` TO `requestmap_id`,
    RENAME COLUMN `request_content` TO `description`,
    ADD COLUMN `created_at`  DATETIME(6)  NULL,
    ADD COLUMN `updated_at`  DATETIME(6)  NULL,
    ADD COLUMN `status`      VARCHAR(20)  NULL,
    ADD COLUMN `file_url`    VARCHAR(500) NULL,
    ADD COLUMN `map_id`      BIGINT       NULL,
    ADD COLUMN `company_id`  INT          NULL;

ALTER TABLE `accommodation_request_map`
    ADD CONSTRAINT `fk_arm_tag_map` FOREIGN KEY (`map_id`)     REFERENCES `accommodation_tag_map` (`map_id`),
    ADD CONSTRAINT `fk_arm_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`company_id`);

-- ────────────────────────────────────────────────────────────
-- 4. coupon_usage
--    PK: coupon_usage_id → usage_id
--    추가: created_at, updated_at (BaseEntity 필드)
-- ────────────────────────────────────────────────────────────
ALTER TABLE `coupon_usage`
    RENAME COLUMN `coupon_usage_id` TO `usage_id`,
    ADD COLUMN `created_at` DATETIME(6) NULL,
    ADD COLUMN `updated_at` DATETIME(6) NULL;

-- ────────────────────────────────────────────────────────────
-- 5. price_calendar
--    PK: price_calendar_id → calendar_id
--    추가: created_at, updated_at (BaseEntity 필드), available_room
-- ────────────────────────────────────────────────────────────
ALTER TABLE `price_calendar`
    RENAME COLUMN `price_calendar_id` TO `calendar_id`,
    ADD COLUMN `created_at`     DATETIME(6) NULL,
    ADD COLUMN `updated_at`     DATETIME(6) NULL,
    ADD COLUMN `available_room` INT         NULL;

-- ────────────────────────────────────────────────────────────
-- 6. review_image
--    PK: review_image_id → image_id
-- ────────────────────────────────────────────────────────────
ALTER TABLE `review_image`
    RENAME COLUMN `review_image_id` TO `image_id`;

-- ────────────────────────────────────────────────────────────
-- 7. room_image
--    PK: room_image_id → image_id
--    추가: sort_order
-- ────────────────────────────────────────────────────────────
ALTER TABLE `room_image`
    RENAME COLUMN `room_image_id` TO `image_id`,
    ADD COLUMN `sort_order` INT NULL;
