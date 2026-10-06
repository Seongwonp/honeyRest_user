#!/usr/bin/env bash
# =============================================================================
# HoneyRest 업데이트: 세 저장소 git pull → 이미지 빌드(바뀐 것만 실제로 다시 빌드) → 재기동 → 오래된 이미지 정리
#
#   sudo ./update.sh              # 백업 → pull → 빌드 → 재기동
#   sudo ./update.sh --no-pull    # pull 없이 현재 작업 트리로 (예: .env 의 [빌드] 값만 바꾼 뒤)
#   sudo ./update.sh --no-backup  # 백업 생략
#
# 동작
#   - 빌드는 BuildKit 레이어 캐시를 쓰므로 코드·빌드 인자가 그대로인 서비스는 몇 초 만에 끝나고 이미지도 바뀌지 않는다.
#     이미지가 실제로 바뀐 서비스만 `docker compose up -d` 가 컨테이너를 다시 만든다.
#   - 이미지가 바뀌면 직전 이미지를 :previous 태그로 남긴다 → 문제가 생기면 ./rollback.sh 로 즉시 되돌린다.
#   - 빌드는 서비스 하나씩 순서대로 한다 (1 OCPU / 6GB 에서 Gradle·Node 동시 빌드로 메모리가 부족해지지 않게).
#   - 사용자 API 에 새 Flyway 마이그레이션이 있으면 재기동 시 자동 적용된다
#     (DB 는 이미지 롤백으로 되돌아가지 않으므로 기본으로 백업을 먼저 한다).
# =============================================================================
set -Eeuo pipefail
# shellcheck source=lib/common.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

DO_PULL=1; DO_BACKUP=1
for arg in "$@"; do
    case "$arg" in
        --no-pull) DO_PULL=0 ;;
        --no-backup) DO_BACKUP=0 ;;
        -h|--help) sed -n '2,17p' "$0"; exit 0 ;;
        *) die "알 수 없는 옵션: $arg" ;;
    esac
done

require_env_file
require_docker

HOST_REPO="$(host_repo_dir)"
REACT_REPO="$(react_repo_dir)"
for repo in "$USER_REPO" "$HOST_REPO" "$REACT_REPO"; do
    [[ -d "$repo/.git" ]] || die "git 저장소가 아닙니다: $repo"
done

# ---- 0) 백업 ------------------------------------------------------------------
if (( DO_BACKUP )) && [[ "$(service_health mysql)" == "healthy" ]]; then
    "$DEPLOY_DIR/backup.sh" || die "백업 실패 — 업데이트를 중단합니다 (--no-backup 으로 건너뛸 수 있음)."
fi

# ---- 1) pull --------------------------------------------------------------------
if (( DO_PULL )); then
    for repo in "$USER_REPO" "$HOST_REPO" "$REACT_REPO"; do
        before="$(git_as_owner "$repo" rev-parse HEAD)"
        log "git pull: $repo"
        git_as_owner "$repo" pull --ff-only || die "$repo pull 실패 (로컬 변경/분기 확인: git -C $repo status)"
        after="$(git_as_owner "$repo" rev-parse HEAD)"
        if [[ "$before" != "$after" ]]; then
            git_as_owner "$repo" log --oneline --no-decorate "$before..$after" | sed 's/^/    /'
        fi
    done
fi

# 관리자 앱이 고정(pin)한 공유 도메인 모듈 커밋으로 서브모듈을 맞춘다 (--remote 아님)
log "서브모듈 동기화: $HOST_REPO/libs/honeyrest-user"
git_as_owner "$HOST_REPO" submodule sync --recursive >/dev/null
git_as_owner "$HOST_REPO" submodule update --init --recursive

# ---- 2) 빌드 (서비스별 순차) ---------------------------------------------------------
image_id() { docker image inspect -f '{{.Id}}' "$1" 2>/dev/null || true; }

CHANGED=()
for svc in user-api host-admin caddy; do
    img="honeyrest/$svc"
    old_id="$(image_id "$img:latest")"
    # 빌드 전에 태그로 붙잡아 둔다: containerd 이미지 저장소(Docker 29 기본)는 latest 태그가 옮겨 가면
    # 이름 없는 옛 이미지를 바로 지우므로, 빌드 후에 ID 로 태그하려 하면 이미 없다.
    if [[ -n "$old_id" ]]; then docker image tag "$img:latest" "$img:pre-update"; fi
    log "빌드: $svc"
    if ! compose build "$svc"; then
        docker image rm "$img:pre-update" >/dev/null 2>&1 || true
        die "$svc 빌드 실패 — 실행 중인 컨테이너는 그대로입니다."
    fi
    new_id="$(image_id "$img:latest")"
    if [[ "$old_id" != "$new_id" ]]; then
        CHANGED+=("$svc")
        # 직전 이미지를 :previous 로 보존 (처음 빌드라면 없음)
        if [[ -n "$old_id" ]]; then docker image tag "$img:pre-update" "$img:previous"; fi
    fi
    # 임시 태그만 제거 (같은 이미지에 latest/previous 태그가 남아 있으면 이미지는 지워지지 않는다)
    docker image rm "$img:pre-update" >/dev/null 2>&1 || true
done
if (( ${#CHANGED[@]} )); then log "새 이미지: ${CHANGED[*]}"; else ok "이미지 변경 없음"; fi

# ---- 3) 재기동 (이미지·설정이 바뀐 컨테이너만 다시 만든다) ----------------------------------------
compose up -d --remove-orphans
wait_healthy user-api 600 || die "user-api 가 healthy 가 되지 않았습니다. 되돌리기: $DEPLOY_DIR/rollback.sh user-api"
wait_healthy host-admin 600 || die "host-admin 이 healthy 가 되지 않았습니다. 되돌리기: $DEPLOY_DIR/rollback.sh host-admin"

# Caddyfile 은 파일 단위 바인드 마운트라, git pull 이 파일을 새로 쓰면(새 inode) 실행 중인 컨테이너는 옛 내용을 계속 본다.
# 내용이 다르면 caddy 컨테이너만 다시 만든다 (인증서는 caddy-data 볼륨에 있으므로 재발급 없음, 1~2초 끊김).
running_caddyfile="$(compose exec -T caddy cat /etc/caddy/Caddyfile 2>/dev/null || true)"
if [[ "$running_caddyfile" != "$(cat "$DEPLOY_DIR/Caddyfile")" ]]; then
    log "Caddyfile 변경 감지 → caddy 컨테이너 재생성"
    compose up -d --no-deps --force-recreate caddy
fi

# ---- 4) 정리: 태그 없는(dangling) 이미지만 삭제. :previous 와 빌드 캐시(Gradle/npm)는 남긴다 -------------
docker image prune -f >/dev/null
ok "업데이트 완료"
compose ps
