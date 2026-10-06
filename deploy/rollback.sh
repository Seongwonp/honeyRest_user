#!/usr/bin/env bash
# =============================================================================
# HoneyRest 이미지 롤백: update.sh 가 남긴 :previous 이미지로 서비스를 되돌린다.
#
#   sudo ./rollback.sh user-api            # 하나만
#   sudo ./rollback.sh user-api host-admin caddy
#
# 주의
#   - 되돌리는 것은 "컨테이너 이미지"뿐이다. DB 스키마(Flyway)·데이터는 되돌아가지 않는다.
#     새 마이그레이션이 이미 적용된 뒤라면 예전 이미지가 validate 에 실패할 수 있다 → docs/DEPLOY.md 의 롤백 절차 참고.
#   - 저장소는 새 커밋에 그대로 있으므로, 다음 update.sh 는 다시 새 코드로 빌드한다.
#     원인을 고칠 때까지 해당 저장소를 이전 커밋으로 checkout 해 두거나 update.sh 를 실행하지 않는다.
# =============================================================================
set -Eeuo pipefail
# shellcheck source=lib/common.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

(( $# > 0 )) || { sed -n '2,13p' "$0"; exit 1; }

require_env_file
require_docker

for svc in "$@"; do
    case "$svc" in
        user-api|host-admin|caddy) ;;
        *) die "롤백할 수 없는 서비스: $svc (user-api | host-admin | caddy)" ;;
    esac
    img="honeyrest/$svc"
    docker image inspect "$img:previous" >/dev/null 2>&1 || die "$img:previous 이미지가 없습니다 (update.sh 로 이미지가 바뀐 적이 없음)."
    # 현재 이미지를 :rolled-back 으로 남겨 두고(원인 분석용) previous 를 latest 로
    if docker image inspect "$img:latest" >/dev/null 2>&1; then
        docker image tag "$img:latest" "$img:rolled-back"
    fi
    docker image tag "$img:previous" "$img:latest"
    log "$svc → 이전 이미지로 교체"
done

# --no-build: compose 가 소스에서 다시 빌드하지 않고 지금 태그된 latest 이미지를 쓰게 한다
compose up -d --no-build "$@"
for svc in "$@"; do
    [[ "$svc" == "caddy" ]] && continue
    wait_healthy "$svc" 600 || die "$svc 가 이전 이미지로도 healthy 가 아닙니다. docker compose logs $svc 확인"
done
ok "롤백 완료: $*"
