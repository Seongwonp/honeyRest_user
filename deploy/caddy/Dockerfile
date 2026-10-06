# syntax=docker/dockerfile:1
# =============================================================================
# Caddy + 사용자 SPA(honeyrest_user_react) 이미지
#   - 1단계(spa-build): Node 20 으로 React SPA 를 프로덕션 빌드 (dist/)
#   - 2단계: caddy:2-alpine 의 /srv 에 dist 를 넣는다. Caddyfile 은 이미지에 굽지 않고
#            docker-compose 에서 ./Caddyfile 을 마운트한다 (설정만 바꿀 때 재빌드 불필요).
#
# 빌드 컨텍스트
#   - 기본 컨텍스트: 이 디렉터리(deploy/caddy) — Dockerfile 외에는 쓰지 않는다
#   - 추가 컨텍스트 "spa": 프론트엔드 저장소 루트 (docker-compose 의 build.additional_contexts.spa)
#     프론트 저장소의 .dockerignore 가 node_modules·dist·.env* 를 제외한다.
#
# 빌드 인자(VITE_*)는 빌드 시점에 번들에 상수로 박힌다 → 값을 바꾸면 이미지를 다시 빌드해야 한다.
#   VITE_BACKEND_URL 은 빈 값으로 고정: SPA 와 API 가 같은 Origin 이므로 axios 가 상대 경로(/api/...)로 요청한다.
#   VITE_E2E 는 절대 설정하지 않는다: 설정되면 토스 위젯 대신 "테스트 결제" 버튼이 번들에 들어간다.
# =============================================================================

FROM node:20-alpine AS spa-build
WORKDIR /app

# 의존성 설치 레이어 (package*.json 이 바뀔 때만 다시 실행)
COPY --from=spa package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm,id=honeyrest-npm \
    npm ci --no-audit --no-fund

COPY --from=spa . .

ARG VITE_ADMIN_URL=""
ARG VITE_OAUTH_REDIRECT_URI=""
ARG VITE_TOSS_WIDGET_CLIENT_KEY=""
ARG VITE_KAKAO_CLIENT_ID=""
ARG VITE_GOOGLE_CLIENT_ID=""
ARG VITE_APP_GOOGLE_MAPS_KEY=""
ARG VITE_GOOGLE_MAP_ID=""
ARG VITE_HOLIDAY_API_KEY=""

# - 로컬 .env 파일이 섞여 들어와도 무시되도록 지운다 (값은 위 ARG 로만 받는다)
# - VITE_E2E 를 환경에서 제거한 채 빌드하고, E2E 전용 버튼 코드가 번들에 남았으면 빌드를 실패시킨다
RUN rm -f .env .env.local .env.production .env.production.local \
 && env -u VITE_E2E VITE_BACKEND_URL="" NODE_OPTIONS="--max-old-space-size=1536" npm run build \
 && test -f dist/index.html \
 && if grep -rq "e2e-test-payment" dist; then echo "VITE_E2E 코드가 운영 번들에 포함되었습니다" >&2; exit 1; fi

# -----------------------------------------------------------------------------
FROM caddy:2-alpine

COPY --from=spa-build /app/dist /srv
