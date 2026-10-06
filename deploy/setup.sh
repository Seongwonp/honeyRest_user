#!/usr/bin/env bash
# =============================================================================
# HoneyRest 서버 최초 설치 (Oracle Cloud Always Free Ampere A1, Ubuntu 26.04 Minimal aarch64 기준)
#
# 사전 준비 (SSH 접속 후, 자세한 순서는 docs/DEPLOY.md)
#   sudo apt-get update && sudo apt-get install -y git
#   sudo mkdir -p /opt/honeyrest && sudo chown "$USER:$USER" /opt/honeyrest
#   git clone https://github.com/Seongwonp/honeyRest_user.git /opt/honeyrest/honeyRest_user
#   sudo /opt/honeyrest/honeyRest_user/deploy/setup.sh
#
# 하는 일 (여러 번 실행해도 안전: 이미 된 단계는 건너뛴다)
#   1) 기본 패키지(curl, git, openssl, cron 등) 설치, 시간대 Asia/Seoul
#   2) 스왑 파일 (기본 4G) — 6GB RAM 에서 Gradle/Node 이미지 빌드 중 메모리 부족 방지
#   3) OS 방화벽에서 TCP 80/443 허용 — Oracle Ubuntu 이미지의 iptables REJECT 규칙 / ufw 모두 처리
#   4) Docker Engine + compose/buildx 플러그인 (공식 저장소, arm64)
#   5) 나머지 두 저장소를 /opt/honeyrest 에 나란히 클론 (--recurse-submodules)
#   6) deploy/.env 생성: 비밀값은 openssl rand 로 생성, 공인 IP 로 SITE_DOMAIN(sslip.io) 자동 설정
#   7) 이미지 빌드(서비스별 순차) → docker compose up -d → healthy 대기
#   8) 초기 데이터 시드 (seed.sh — 기초 데이터는 한 번만 적재됨)
#   9) cron: 5분마다 헬스 체크, 매일 04:30 백업
#
# 환경변수로 바꿀 수 있는 값
#   BASE_DIR=/opt/honeyrest  SWAP_SIZE=4G(0 이면 건너뜀)  SITE_DOMAIN=(자동)  SKIP_SEED=1  SKIP_CRON=1  SKIP_FIREWALL=1
#   HOST_REPO_URL / REACT_REPO_URL / HOST_BRANCH(main) / REACT_BRANCH(master)
# =============================================================================
set -Eeuo pipefail

# root 로 다시 실행
if [[ "$(id -u)" -ne 0 ]]; then
    exec sudo -E bash "$0" "$@"
fi

# shellcheck source=lib/common.sh
source "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")/lib/common.sh"

SWAP_SIZE="${SWAP_SIZE:-4G}"
HOST_REPO_URL="${HOST_REPO_URL:-https://github.com/Seongwonp/honeyRest_host.git}"
REACT_REPO_URL="${REACT_REPO_URL:-https://github.com/Seongwonp/honeyrest_user_react.git}"
HOST_BRANCH="${HOST_BRANCH:-main}"
REACT_BRANCH="${REACT_BRANCH:-master}"
# 저장소·.env 소유자: sudo 를 실행한 SSH 사용자 (없으면 사용자 저장소의 소유자)
TARGET_USER="${SUDO_USER:-}"
if [[ -z "$TARGET_USER" || "$TARGET_USER" == "root" ]]; then
    TARGET_USER="$(stat -c %U "$USER_REPO")"
fi

[[ "$(uname -m)" == "aarch64" || "$(uname -m)" == "x86_64" ]] || warn "검증되지 않은 아키텍처: $(uname -m)"
# shellcheck disable=SC1091
. /etc/os-release
[[ "${ID:-}" == "ubuntu" ]] || warn "Ubuntu 가 아닙니다 (${PRETTY_NAME:-unknown}). Docker 설치 단계가 실패할 수 있습니다."
export DEBIAN_FRONTEND=noninteractive

# ---- 1) 기본 패키지 ---------------------------------------------------------------
log "1) 기본 패키지 설치"
apt-get update -y -q
apt-get install -y -q ca-certificates curl gnupg git openssl cron tar gzip iptables
systemctl enable --now cron >/dev/null 2>&1 || true
if command -v timedatectl >/dev/null 2>&1; then
    timedatectl set-timezone Asia/Seoul 2>/dev/null || warn "시간대 설정 실패 (UTC 유지)"
fi

# ---- 2) 스왑 ------------------------------------------------------------------
log "2) 스왑 파일 (${SWAP_SIZE})"
if [[ "$SWAP_SIZE" == "0" ]]; then
    warn "스왑 단계 건너뜀 (SWAP_SIZE=0)"
elif swapon --show --noheadings | grep -q .; then
    ok "스왑이 이미 있습니다: $(swapon --show --noheadings | awk '{print $1" "$3}' | tr '\n' ' ')"
else
    if [[ ! -f /swapfile ]]; then
        fallocate -l "$SWAP_SIZE" /swapfile 2>/dev/null || dd if=/dev/zero of=/swapfile bs=1M count="$(( $(numfmt --from=iec "$SWAP_SIZE") / 1048576 ))" status=none
        chmod 600 /swapfile
        mkswap /swapfile >/dev/null
    fi
    swapon /swapfile
    grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
    ok "스왑 활성화: $SWAP_SIZE"
fi
# 메모리가 남을 때는 스왑을 거의 쓰지 않게 (빌드 피크 때만 사용)
cat > /etc/sysctl.d/99-honeyrest.conf <<'EOF'
vm.swappiness=10
vm.vfs_cache_pressure=50
EOF
sysctl -q --system >/dev/null 2>&1 || true

# ---- 3) OS 방화벽 (80/443) ----------------------------------------------------------
# OCI 콘솔의 보안 목록(Security List) 인그레스 규칙은 이 스크립트가 열 수 없다 → docs/DEPLOY.md 2장(사전 준비)에서 직접 연다.
open_firewall() {
    if command -v ufw >/dev/null 2>&1 && ufw status 2>/dev/null | grep -q '^Status: active'; then
        ufw allow 80/tcp >/dev/null
        ufw allow 443/tcp >/dev/null
        ok "ufw: 80/tcp, 443/tcp 허용"
        return
    fi

    # Oracle Ubuntu 이미지: /etc/iptables/rules.v4 에 "-A INPUT -j REJECT --reject-with icmp-host-prohibited" 가 있다.
    # 실행 중인 규칙: REJECT 앞에 ACCEPT 를 끼워 넣는다 (이미 있으면 건너뜀)
    local port line
    for port in 80 443; do
        if ! iptables -C INPUT -p tcp -m state --state NEW -m tcp --dport "$port" -j ACCEPT 2>/dev/null; then
            line="$(iptables -L INPUT --line-numbers -n | awk '$2 == "REJECT" { print $1; exit }')"
            if [[ -n "$line" ]]; then
                iptables -I INPUT "$line" -p tcp -m state --state NEW -m tcp --dport "$port" -j ACCEPT
            else
                iptables -A INPUT -p tcp -m state --state NEW -m tcp --dport "$port" -j ACCEPT
            fi
        fi
    done

    # 재부팅 후에도 유지: 저장 파일을 직접 수정한다.
    # (netfilter-persistent save 는 Docker 가 만든 규칙까지 저장해 재부팅 시 꼬일 수 있으므로 쓰지 않는다)
    local rules=/etc/iptables/rules.v4
    if [[ -f "$rules" ]]; then
        for port in 80 443; do
            if ! grep -Eq -- "--dport $port -j ACCEPT" "$rules"; then
                if grep -q -- '^-A INPUT -j REJECT' "$rules"; then
                    sed -i "0,/^-A INPUT -j REJECT/s//-A INPUT -p tcp -m state --state NEW -m tcp --dport $port -j ACCEPT\n&/" "$rules"
                else
                    sed -i "0,/^COMMIT/s//-A INPUT -p tcp -m state --state NEW -m tcp --dport $port -j ACCEPT\n&/" "$rules"
                fi
            fi
        done
        ok "iptables: 80/443 허용 (실행 중 규칙 + $rules)"
    else
        # rules.v4 가 없는 이미지: iptables-persistent 로 지금(= Docker 설치 전) 상태를 저장한다
        echo iptables-persistent iptables-persistent/autosave_v4 boolean true | debconf-set-selections
        echo iptables-persistent iptables-persistent/autosave_v6 boolean true | debconf-set-selections
        apt-get install -y -q iptables-persistent >/dev/null
        if ! systemctl is-active --quiet docker 2>/dev/null; then
            netfilter-persistent save >/dev/null 2>&1 || true
        fi
        ok "iptables: 80/443 허용 (iptables-persistent)"
    fi
}
if [[ "${SKIP_FIREWALL:-0}" == "1" ]]; then
    warn "3) 방화벽 단계 건너뜀 (SKIP_FIREWALL=1)"
else
    log "3) OS 방화벽 80/443 허용"
    open_firewall
fi

# ---- 4) Docker -----------------------------------------------------------------
install_docker() {
    if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1 && docker buildx version >/dev/null 2>&1; then
        ok "Docker 가 이미 설치되어 있습니다: $(docker --version)"
        return
    fi
    local arch codename
    arch="$(dpkg --print-architecture)"
    codename="${VERSION_CODENAME:-${UBUNTU_CODENAME:-}}"
    # 새 Ubuntu 릴리스가 Docker 저장소에 아직 없으면 직전 LTS(noble) 패키지를 쓴다
    if ! curl -fsSI "https://download.docker.com/linux/ubuntu/dists/${codename}/Release" >/dev/null 2>&1; then
        warn "Docker 저장소에 '${codename}' 이 없어 noble 저장소를 사용합니다."
        codename="noble"
    fi
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc
    echo "deb [arch=${arch} signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${codename} stable" \
        > /etc/apt/sources.list.d/docker.list
    apt-get update -y -q
    if ! apt-get install -y -q docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin; then
        warn "공식 저장소 설치 실패 → Ubuntu 패키지(docker.io, docker-compose-v2, docker-buildx)로 설치합니다."
        rm -f /etc/apt/sources.list.d/docker.list
        apt-get update -y -q
        apt-get install -y -q docker.io docker-compose-v2 docker-buildx
    fi
}
log "4) Docker Engine + compose 플러그인"
install_docker
systemctl enable --now docker
if [[ "$TARGET_USER" != "root" ]] && ! id -nG "$TARGET_USER" | grep -qw docker; then
    usermod -aG docker "$TARGET_USER"
    warn "$TARGET_USER 를 docker 그룹에 추가했습니다. sudo 없이 docker 를 쓰려면 다시 로그인하세요."
fi
docker compose version >/dev/null || die "docker compose 플러그인을 쓸 수 없습니다."
ok "$(docker --version) / $(docker compose version --short 2>/dev/null || echo compose)"

# ---- 5) 저장소 클론 ---------------------------------------------------------------
clone_repo() {
    local url="$1" dir="$2" branch="$3"
    if [[ -d "$dir/.git" ]]; then
        ok "이미 있음: $dir"
    else
        log "클론: $url → $dir"
        sudo -u "$TARGET_USER" -H git clone --recurse-submodules --branch "$branch" "$url" "$dir"
    fi
}
log "5) 저장소 준비 ($BASE_DIR)"
chown "$TARGET_USER:" "$BASE_DIR" 2>/dev/null || true
clone_repo "$HOST_REPO_URL" "$BASE_DIR/honeyRest_host" "$HOST_BRANCH"
clone_repo "$REACT_REPO_URL" "$BASE_DIR/honeyrest_user_react" "$REACT_BRANCH"
git_as_owner "$BASE_DIR/honeyRest_host" submodule update --init --recursive
[[ -f "$BASE_DIR/honeyRest_host/libs/honeyrest-user/honeyrest-domain/build.gradle" ]] \
    || die "관리자 저장소 서브모듈(libs/honeyrest-user)이 비어 있습니다."

# ---- 6) .env ------------------------------------------------------------------
detect_public_ip() {
    local ip url
    for url in https://ifconfig.me https://api.ipify.org https://checkip.amazonaws.com; do
        ip="$(curl -fsS4 --max-time 10 "$url" 2>/dev/null | tr -d '[:space:]')" || true
        if [[ "$ip" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then printf '%s' "$ip"; return 0; fi
    done
    return 1
}
gen_hex() { openssl rand -hex "$1"; }
# 사람이 입력할 데모 비밀번호: 영숫자 16자
gen_password() { openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | head -c 16; }

log "6) deploy/.env"
FIRST_INSTALL=0
if [[ ! -f "$ENV_FILE" ]]; then
    FIRST_INSTALL=1
    install -m 600 -o "$TARGET_USER" "$DEPLOY_DIR/.env.example" "$ENV_FILE"
    env_set MYSQL_PASSWORD "$(gen_hex 24)"
    env_set MYSQL_ROOT_PASSWORD "$(gen_hex 24)"
    env_set USER_JWT_SECRET "$(gen_hex 48)"
    env_set HOST_JWT_SECRET "$(gen_hex 48)"
    env_set DEMO_COMPANY_PASSWORD "$(gen_password)"
    env_set DEMO_ADMIN_PASSWORD "$(gen_password)"
    ok ".env 생성 (비밀값 자동 생성, 권한 600)"
else
    ok ".env 가 이미 있어 그대로 사용합니다 (비밀값을 다시 만들지 않음)"
fi

CURRENT_DOMAIN="$(env_get SITE_DOMAIN)"
if [[ -n "${SITE_DOMAIN:-}" ]]; then
    env_set SITE_DOMAIN "$SITE_DOMAIN"
elif [[ -z "$CURRENT_DOMAIN" || "$CURRENT_DOMAIN" == CHANGE_ME* ]]; then
    PUBLIC_IP="$(detect_public_ip)" || die "공인 IP 를 알아내지 못했습니다. SITE_DOMAIN=<IP-하이픈>.sslip.io 로 지정해 다시 실행하세요."
    env_set SITE_DOMAIN "${PUBLIC_IP//./-}.sslip.io"
fi
SITE="$(env_get SITE_DOMAIN)"
ok "SITE_DOMAIN=$SITE"

# ---- 7) 빌드 & 기동 ---------------------------------------------------------------
log "7) 이미지 빌드 (서비스별 순차 — 첫 빌드는 1 OCPU 에서 15~30분 걸릴 수 있습니다)"
for svc in user-api host-admin caddy; do
    log "빌드: $svc"
    compose build "$svc"
done
log "기동: docker compose up -d"
compose up -d --remove-orphans
wait_healthy user-api 900 || die "user-api 기동 실패 (docker compose logs user-api)"
wait_healthy host-admin 900 || die "host-admin 기동 실패 (docker compose logs host-admin)"

# ---- 8) 시드 ------------------------------------------------------------------
if [[ "${SKIP_SEED:-0}" == "1" ]]; then
    warn "8) 시드 건너뜀 (SKIP_SEED=1)"
else
    log "8) 초기 데이터 시드"
    "$DEPLOY_DIR/seed.sh"
fi

# ---- 9) cron ------------------------------------------------------------------
if [[ "${SKIP_CRON:-0}" == "1" ]]; then
    warn "9) cron 건너뜀 (SKIP_CRON=1)"
else
    log "9) cron 등록: /etc/cron.d/honeyrest"
    cat > /etc/cron.d/honeyrest <<EOF
# HoneyRest (deploy/setup.sh 가 생성) — 시간은 서버 시간대(Asia/Seoul) 기준
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
# 5분마다 공개 주소로 사용자 API·관리자 앱 헬스 체크 (Caddy·인증서까지 함께 확인, 실패 시 syslog 에 기록: journalctl -t honeyrest-health)
*/5 * * * * root curl -fsS --max-time 20 -o /dev/null https://${SITE}/api/banner/list && curl -fsS --max-time 20 -o /dev/null https://admin.${SITE}/actuator/health || logger -t honeyrest-health "health check failed: ${SITE}"
# 매일 04:30 DB·업로드 백업 (최근 7개 보관)
30 4 * * * root ${DEPLOY_DIR}/backup.sh >> /var/log/honeyrest-backup.log 2>&1
EOF
    chmod 644 /etc/cron.d/honeyrest
    ok "cron 등록 완료"
fi

# ---- 완료 ----------------------------------------------------------------------
cat <<EOF

${C_OK}==================== 설치 완료 ====================${C_END}
  사용자 사이트 : https://${SITE}
  관리자 사이트 : https://admin.${SITE}
EOF
if [[ "$(env_get HOST_SPRING_PROFILES)" == *local-demo* ]]; then
    cat <<EOF
  관리자 데모 계정 (비밀번호는 $ENV_FILE 에 저장되어 있음)
    업체 관리자 : contact@honeyrest.com / $(env_get DEMO_COMPANY_PASSWORD)
    총관리자   : admin@honeyrest.com / $(env_get DEMO_ADMIN_PASSWORD)
EOF
fi
cat <<EOF

  - HTTPS 인증서는 첫 접속 전후로 Caddy 가 발급합니다(수십 초). 실패하면: docker compose -f $DEPLOY_DIR/docker-compose.yml logs caddy
  - OCI 콘솔 보안 목록에서 TCP 80/443 인그레스를 열었는지 확인하세요.
  - 결제(TOSS_*), 메일(MAIL_*), 소셜 로그인 키는 $ENV_FILE 에 채운 뒤: sudo $DEPLOY_DIR/update.sh --no-pull
EOF
(( FIRST_INSTALL )) && echo "  - 처음 생성된 .env 는 백업해 두세요 (DB 비밀번호를 잃으면 데이터에 접근할 수 없습니다)."
exit 0
