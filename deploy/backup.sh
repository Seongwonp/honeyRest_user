#!/usr/bin/env bash
# =============================================================================
# HoneyRest 백업: MySQL 덤프 + uploads 볼륨 → /opt/honeyrest/backups (최근 KEEP 개만 보관)
#
#   sudo ./backup.sh
#   KEEP=14 sudo -E ./backup.sh      # 보관 개수 변경 (기본 7)
#
# cron 예시 (setup.sh 가 /etc/cron.d/honeyrest 에 설치한다: 매일 04:30)
#   30 4 * * * root /opt/honeyrest/honeyRest_user/deploy/backup.sh >> /var/log/honeyrest-backup.log 2>&1
#
# 복원은 docs/DEPLOY.md 의 "백업과 복원" 참고.
# 백업 파일에는 회원 정보(비밀번호 해시 포함)가 들어 있으므로 600 권한으로 저장한다.
# 서버 밖(로컬 PC, OCI Object Storage 등)에도 주기적으로 복사해 두는 것을 권장한다.
# =============================================================================
set -Eeuo pipefail
# shellcheck source=lib/common.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

KEEP="${KEEP:-7}"
[[ "$KEEP" =~ ^[1-9][0-9]*$ ]] || die "KEEP 은 1 이상의 정수여야 합니다: $KEEP"

require_env_file
require_docker

umask 077
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

STAMP="$(date +%Y%m%d-%H%M%S)"
DB_NAME="$(env_get MYSQL_DATABASE honeyrest_db)"
DB_FILE="$BACKUP_DIR/${DB_NAME}-${STAMP}.sql.gz"
UPLOADS_FILE="$BACKUP_DIR/uploads-${STAMP}.tar.gz"

[[ "$(service_health mysql)" == "healthy" ]] || die "mysql 컨테이너가 healthy 가 아닙니다 (docker compose ps 확인)."

# ---- MySQL ----------------------------------------------------------------
# --single-transaction: InnoDB 를 잠그지 않고 일관된 스냅샷 (서비스 중단 없음)
log "MySQL 덤프 → $DB_FILE"
# shellcheck disable=SC2016  # 변수는 컨테이너 안의 sh 가 펼친다 (의도적으로 작은따옴표)
if compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot \
        --single-transaction --quick --routines --triggers --events \
        --no-tablespaces --set-gtid-purged=OFF --default-character-set=utf8mb4 \
        "$MYSQL_DATABASE"' | gzip -6 > "$DB_FILE.partial"; then
    mv "$DB_FILE.partial" "$DB_FILE"
else
    rm -f "$DB_FILE.partial"
    die "MySQL 덤프 실패"
fi
# 덤프가 끝까지 쓰였는지 확인 (mysqldump 는 마지막 줄에 "-- Dump completed" 를 남긴다)
gzip -dc "$DB_FILE" | tail -n 1 | grep -q 'Dump completed' || die "덤프 파일이 불완전합니다: $DB_FILE"
ok "MySQL 덤프 $(du -h "$DB_FILE" | cut -f1)"

# ---- uploads 볼륨 (업로드 이미지) ---------------------------------------------------
if [[ "$(service_health user-api)" != "missing" ]]; then
    log "uploads 볼륨 → $UPLOADS_FILE"
    if compose exec -T user-api tar -C /app -czf - uploads > "$UPLOADS_FILE.partial"; then
        mv "$UPLOADS_FILE.partial" "$UPLOADS_FILE"
        ok "uploads $(du -h "$UPLOADS_FILE" | cut -f1)"
    else
        rm -f "$UPLOADS_FILE.partial"
        warn "uploads 백업 실패 (DB 백업은 완료)"
    fi
else
    warn "user-api 컨테이너가 없어 uploads 백업을 건너뜁니다."
fi

# ---- 보관 개수 정리 -------------------------------------------------------------
prune() {
    local pattern="$1"
    # 이름에 타임스탬프가 있으므로 이름 역순 = 최신순
    find "$BACKUP_DIR" -maxdepth 1 -type f -name "$pattern" -printf '%f\n' | sort -r | tail -n +"$((KEEP + 1))" |
        while IFS= read -r old; do rm -f -- "$BACKUP_DIR/$old"; log "오래된 백업 삭제: $old"; done
}
prune "${DB_NAME}-*.sql.gz"
prune "uploads-*.tar.gz"

ok "백업 완료 (보관: 최근 ${KEEP}개, 위치: $BACKUP_DIR)"
