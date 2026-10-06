#!/usr/bin/env bash
# =============================================================================
# deploy/*.sh 공용 함수. 직접 실행하지 않고 source 해서 쓴다.
#   DEPLOY_DIR  : 이 저장소의 deploy/ 절대 경로
#   USER_REPO   : 사용자 API 저장소 루트 (= DEPLOY_DIR/..)
#   BASE_DIR    : 세 저장소가 나란히 있는 상위 디렉터리 (기본 /opt/honeyrest)
# =============================================================================

COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$COMMON_DIR/.." && pwd)"
USER_REPO="$(cd "$DEPLOY_DIR/.." && pwd)"
BASE_DIR="${BASE_DIR:-$(cd "$USER_REPO/.." && pwd)}"
ENV_FILE="$DEPLOY_DIR/.env"
BACKUP_DIR="${BACKUP_DIR:-$BASE_DIR/backups}"

if [[ -t 1 ]]; then
    C_INFO=$'\033[1;34m'; C_OK=$'\033[1;32m'; C_WARN=$'\033[1;33m'; C_ERR=$'\033[1;31m'; C_END=$'\033[0m'
else
    C_INFO=""; C_OK=""; C_WARN=""; C_ERR=""; C_END=""
fi

log()  { printf '%s[honeyrest]%s %s\n' "$C_INFO" "$C_END" "$*"; }
ok()   { printf '%s[  OK  ]%s %s\n' "$C_OK" "$C_END" "$*"; }
warn() { printf '%s[ WARN ]%s %s\n' "$C_WARN" "$C_END" "$*" >&2; }
die()  { printf '%s[ FAIL ]%s %s\n' "$C_ERR" "$C_END" "$*" >&2; exit 1; }

# .env 에서 값 하나를 읽는다 (source 하지 않음: 따옴표·공백이 있어도 안전).
#   env_get KEY [기본값]
env_get() {
    local key="$1" default="${2-}" value
    [[ -f "$ENV_FILE" ]] || { printf '%s' "$default"; return; }
    value="$(awk -v k="$key" '
        /^[[:space:]]*#/ { next }
        index($0, k "=") == 1 { v = substr($0, length(k) + 2) }
        END { print v }' "$ENV_FILE")"
    # 감싼 따옴표 제거
    if [[ "$value" =~ ^\"(.*)\"$ ]] || [[ "$value" =~ ^\'(.*)\'$ ]]; then
        value="${BASH_REMATCH[1]}"
    fi
    if [[ -z "$value" ]]; then value="$default"; fi
    printf '%s' "$value"
}

# .env 의 KEY=... 줄을 VALUE 로 바꾼다 (없으면 끝에 추가). 값은 그대로 기록된다.
env_set() {
    local key="$1" value="$2" tmp
    tmp="$(mktemp "${ENV_FILE}.XXXXXX")"
    # 값은 ENVIRON 으로 넘긴다 (awk -v 는 역슬래시 이스케이프를 해석하므로)
    K="$key" V="$value" awk '
        BEGIN { k = ENVIRON["K"]; v = ENVIRON["V"]; done = 0 }
        index($0, k "=") == 1 { print k "=" v; done = 1; next }
        { print }
        END { if (!done) print k "=" v }' "$ENV_FILE" > "$tmp"
    # 원본 권한·소유자 유지
    chmod --reference="$ENV_FILE" "$tmp" 2>/dev/null || chmod 600 "$tmp"
    chown --reference="$ENV_FILE" "$tmp" 2>/dev/null || true
    mv "$tmp" "$ENV_FILE"
}

# 로컬 전용 이미지라 BuildKit 기본 provenance 증명(attestation)이 필요 없다. 이것이 켜져 있으면 빌드 시각이 들어가
# 코드가 그대로여도 매번 이미지 ID 가 바뀌고, update.sh 가 모든 컨테이너를 불필요하게 다시 만든다.
export BUILDX_NO_DEFAULT_ATTESTATIONS=1

# deploy/ 디렉터리 기준 docker compose
# 서버별 조정(메모리, 포트 등)은 추적되지 않는 deploy/docker-compose.override.yml 에 두면 함께 적용된다.
compose() {
    local files=(-f "$DEPLOY_DIR/docker-compose.yml")
    if [[ -f "$DEPLOY_DIR/docker-compose.override.yml" ]]; then
        files+=(-f "$DEPLOY_DIR/docker-compose.override.yml")
    fi
    docker compose --project-directory "$DEPLOY_DIR" "${files[@]}" --env-file "$ENV_FILE" "$@"
}

require_env_file() {
    [[ -f "$ENV_FILE" ]] || die "$ENV_FILE 이 없습니다. setup.sh 를 먼저 실행하거나 .env.example 을 복사해 값을 채우세요."
}

require_docker() {
    command -v docker >/dev/null 2>&1 || die "docker 가 설치되어 있지 않습니다 (setup.sh 참고)."
    docker info >/dev/null 2>&1 || die "docker 데몬에 접근할 수 없습니다. sudo 로 실행하거나 docker 그룹에 추가한 뒤 다시 로그인하세요."
    docker compose version >/dev/null 2>&1 || die "docker compose 플러그인이 없습니다 (setup.sh 참고)."
}

# 서비스 컨테이너의 헬스 상태 (healthy / starting / unhealthy / none / missing)
service_health() {
    local id
    id="$(compose ps -q "$1" 2>/dev/null | head -n1)"
    [[ -n "$id" ]] || { echo missing; return; }
    docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$id" 2>/dev/null || echo missing
}

# wait_healthy 서비스 [제한 초]
wait_healthy() {
    local service="$1" timeout="${2:-600}" waited=0 status
    log "$service 가 healthy 가 될 때까지 기다립니다 (최대 ${timeout}초)..."
    while true; do
        status="$(service_health "$service")"
        case "$status" in
            healthy) ok "$service healthy (${waited}초)"; return 0 ;;
            unhealthy) warn "$service unhealthy — 최근 로그:"; compose logs --tail=60 "$service" >&2 || true; return 1 ;;
        esac
        if (( waited >= timeout )); then
            warn "$service 대기 시간 초과 (상태: $status) — 최근 로그:"
            compose logs --tail=60 "$service" >&2 || true
            return 1
        fi
        sleep 5; waited=$((waited + 5))
    done
}

# 저장소 소유자 권한으로 git 실행 (root 로 실행해도 'dubious ownership' 오류·root 소유 파일이 생기지 않게)
#   git_as_owner 저장소경로 git인자...
git_as_owner() {
    local repo="$1"; shift
    local owner
    owner="$(stat -c %U "$repo")"
    if [[ "$(id -u)" -eq 0 && "$owner" != "root" ]]; then
        sudo -u "$owner" -H git -C "$repo" "$@"
    else
        git -C "$repo" "$@"
    fi
}

# .env 의 상대 경로(HOST_REPO_DIR 등)를 deploy/ 기준 절대 경로로
resolve_repo_dir() {
    local rel="$1"
    if [[ "$rel" = /* ]]; then printf '%s' "$rel"; else (cd "$DEPLOY_DIR" && cd "$rel" 2>/dev/null && pwd) || printf '%s/%s' "$DEPLOY_DIR" "$rel"; fi
}

host_repo_dir()  { resolve_repo_dir "$(env_get HOST_REPO_DIR ../../honeyRest_host)"; }
react_repo_dir() { resolve_repo_dir "$(env_get REACT_REPO_DIR ../../honeyrest_user_react)"; }
