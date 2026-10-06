#!/usr/bin/env bash
# =============================================================================
# HoneyRest 초기 데이터 시드 (deploy/ 의 compose 스택이 떠 있는 상태에서 실행)
#
#   sudo ./seed.sh                 # 첫 설치: 기초 데이터 + 데모 리뷰 + 이미지 경로 치환 + 플레이스홀더 복사
#   sudo ./seed.sh --demo-refresh  # 데모 데이터만 다시 (예약 가능 달력을 오늘부터 90일로 연장, 반복 실행 안전)
#
# 단계 (순서가 중요하다)
#   1) 기초 데이터: 관리자 저장소 db/seed/*.sql  — 단 한 번만 (FK 순서 고정)
#        insert.sql          지역·카테고리·태그·업체·배너·쿠폰·이벤트
#        insert_pk_1-20.sql  숙소 1~20 (+ 이미지·태그 매핑·객실·취소 규정)
#        insert_pk_21-30.sql 숙소 21~30 (자동 증가 PK 가 21~30 이 되어야 하므로 반드시 빈 테이블에서)
#        insert_pk_31-33.sql 숙소 31~33
#      네 파일과 완료 표시(deploy_seed_history)를 한 트랜잭션으로 넣는다 → 중간 실패 시 전부 롤백.
#      완료 표시가 있거나, 표시가 없어도 숙소 데이터가 이미 있으면 건너뛴다 (중복 PK 방지).
#   2) scripts/seed-demo-data.sql   데모 리뷰 사용자·리뷰, 90일 가격 달력, min_price·rating 재계산 (반복 실행 안전)
#   3) scripts/seed-local-images.sql 만료된 Firebase 이미지 URL → /uploads/placeholder/*.svg (반복 실행 안전)
#   4) uploads/placeholder/*.svg 를 uploads 볼륨에 복사
#   5) Redis 검색 캐시 세대 번호 증가 + 배너 캐시 삭제 (새 데이터가 바로 보이도록)
#   6) scripts/audit-data-quality.sql 요약 출력 (읽기 전용)
# =============================================================================
set -Eeuo pipefail
# shellcheck source=lib/common.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

MODE="full"
case "${1:-}" in
    "") ;;
    --demo-refresh) MODE="demo" ;;
    -h|--help) sed -n '2,25p' "$0"; exit 0 ;;
    *) die "알 수 없는 옵션: $1 (--demo-refresh | --help)" ;;
esac

require_env_file
require_docker

HOST_REPO="$(host_repo_dir)"
SEED_DIR="$HOST_REPO/db/seed"
SCRIPTS_DIR="$USER_REPO/scripts"
BASE_SEED_FILES=(insert.sql insert_pk_1-20.sql insert_pk_21-30.sql insert_pk_31-33.sql)
MARKER="host-db-seed-v1"

# 컨테이너 안의 mysql 클라이언트 (root 비밀번호는 컨테이너 환경변수에서만 읽는다: 명령줄·호스트에 노출 안 함)
mysql_in() {
    # shellcheck disable=SC2016  # 변수는 컨테이너 안의 sh 가 펼친다 (의도적으로 작은따옴표)
    compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 "$@" "$MYSQL_DATABASE"' mysql "$@"
}
mysql_query() { mysql_in -N -B <<<"$1"; }

# ---- 사전 확인 ----------------------------------------------------------------
for f in "${BASE_SEED_FILES[@]}"; do
    [[ -f "$SEED_DIR/$f" ]] || die "시드 파일이 없습니다: $SEED_DIR/$f (HOST_REPO_DIR 확인)"
done
for f in seed-demo-data.sql seed-local-images.sql audit-data-quality.sql; do
    [[ -f "$SCRIPTS_DIR/$f" ]] || die "스크립트가 없습니다: $SCRIPTS_DIR/$f"
done
[[ -d "$USER_REPO/uploads/placeholder" ]] || die "플레이스홀더 이미지가 없습니다: $USER_REPO/uploads/placeholder"

wait_healthy mysql 300 || die "MySQL 이 준비되지 않았습니다."
# 스키마는 사용자 API 의 Flyway 가 만든다 → 사용자 API 가 healthy 여야 테이블이 있다
wait_healthy user-api 600 || die "user-api 가 healthy 가 아닙니다 (Flyway 마이그레이션 실패 여부를 로그에서 확인)."

if [[ "$(mysql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'accommodation';")" != "1" ]]; then
    die "accommodation 테이블이 없습니다. 사용자 API 의 Flyway 마이그레이션이 끝났는지 확인하세요."
fi

# ---- 1) 기초 데이터 (한 번만) ------------------------------------------------------
if [[ "$MODE" == "full" ]]; then
    mysql_query "CREATE TABLE IF NOT EXISTS deploy_seed_history (
        name VARCHAR(100) NOT NULL PRIMARY KEY,
        applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;" >/dev/null

    applied="$(mysql_query "SELECT COUNT(*) FROM deploy_seed_history WHERE name = '$MARKER';")"
    existing="$(mysql_query "SELECT COUNT(*) FROM accommodation;")"

    if [[ "$applied" != "0" ]]; then
        ok "기초 데이터는 이미 적재되어 있습니다 ($MARKER) — 건너뜀"
    elif [[ "$existing" != "0" ]]; then
        warn "완료 표시는 없지만 숙소 데이터가 ${existing}건 있습니다 — 중복 PK 를 피하려고 기초 데이터 적재를 건너뜁니다."
    else
        log "기초 데이터 적재: ${BASE_SEED_FILES[*]}"
        {
            echo "SET autocommit = 0;"
            echo "START TRANSACTION;"
            for f in "${BASE_SEED_FILES[@]}"; do
                echo "-- ==== $f ===="
                cat "$SEED_DIR/$f"
                echo ""
            done
            echo "INSERT INTO deploy_seed_history (name) VALUES ('$MARKER');"
            echo "COMMIT;"
        } | mysql_in || die "기초 데이터 적재 실패 — 트랜잭션이 롤백되었습니다. 위 오류를 확인하세요."
        ok "기초 데이터 적재 완료 (숙소 $(mysql_query "SELECT COUNT(*) FROM accommodation;")건, 객실 $(mysql_query "SELECT COUNT(*) FROM room;")건)"
    fi
fi

# ---- 2) 데모 리뷰·가격 달력·파생 값 재계산 (반복 실행 안전) ------------------------------------
log "데모 데이터 적용: scripts/seed-demo-data.sql"
mysql_in <"$SCRIPTS_DIR/seed-demo-data.sql" >/dev/null || die "seed-demo-data.sql 실패"
ok "데모 리뷰 $(mysql_query "SELECT COUNT(*) FROM review r JOIN reservation v ON v.reservation_id = r.reservation_id WHERE v.reservation_number LIKE 'DEMO-REVIEW-%';")건, 가격 달력은 오늘부터 90일"

if [[ "$MODE" == "full" ]]; then
    # ---- 3) 이미지 URL 치환 (반복 실행 안전) ------------------------------------------
    log "만료된 Firebase 이미지 URL → 로컬 플레이스홀더: scripts/seed-local-images.sql"
    mysql_in <"$SCRIPTS_DIR/seed-local-images.sql" | sed 's/^/    /' || die "seed-local-images.sql 실패"

    # ---- 4) 플레이스홀더 파일을 uploads 볼륨으로 (앱 사용자 권한으로 풀어 소유자를 맞춘다) -------------
    log "uploads/placeholder → uploads 볼륨 복사"
    tar -C "$USER_REPO/uploads" -cf - placeholder | compose exec -T user-api tar -C /app/uploads -xf - \
        || die "플레이스홀더 복사 실패"
    ok "플레이스홀더 $(compose exec -T user-api sh -c 'ls /app/uploads/placeholder | wc -l' | tr -d '[:space:]')개"
fi

# ---- 5) 캐시 무효화 ------------------------------------------------------------
compose exec -T redis sh -c "redis-cli INCR search:recommend:version >/dev/null; redis-cli --scan --pattern 'banners::*' | xargs -r redis-cli DEL >/dev/null" \
    || warn "Redis 캐시 무효화 실패 (최대 1시간 뒤 자연 만료)"

# ---- 6) 데이터 품질 요약 (읽기 전용) --------------------------------------------------
log "데이터 품질 점검 (scripts/audit-data-quality.sql, 0 이 정상인 항목: *_mismatch, rooms_without_images, orphan_*)"
mysql_in -t <"$SCRIPTS_DIR/audit-data-quality.sql" | sed 's/^/    /' || warn "점검 쿼리 실패 (시드 결과에는 영향 없음)"

ok "시드 완료"
