# syntax=docker/dockerfile:1
# =============================================================================
# 사용자 API (honeyRest_user) 운영 이미지
#   빌드: docker compose -f deploy/docker-compose.yml build user-api   (빌드 컨텍스트 = 이 저장소 루트)
#   - 1단계(build): JDK 17 + Gradle Wrapper 로 bootJar 생성 (테스트는 CI 에서 실행하므로 생략)
#   - 2단계(runtime): JRE 17 + 비루트 사용자로 실행
#   eclipse-temurin 은 arm64(aarch64) 이미지를 제공하므로 Oracle Cloud Ampere(ARM) VM 에서 그대로 빌드된다.
# =============================================================================

FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 1 OCPU / 6GB 서버에서 직접 빌드하므로 Gradle 메모리를 보수적으로 제한한다.
#  - --no-daemon: 빌드 후 데몬이 메모리를 붙잡고 남지 않게 함
#  - org.gradle.jvmargs: 컴파일러 데몬/워커 힙 상한
ENV GRADLE_OPTS="-Xmx512m -Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx1g -Dorg.gradle.workers.max=2 -Dorg.gradle.welcome=never"

# 의존성 해석에 필요한 빌드 스크립트만 먼저 복사 → 소스만 바뀌면 이 레이어(Gradle 배포판 다운로드)는 캐시 재사용
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY honeyrest-domain/build.gradle honeyrest-domain/build.gradle
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew

COPY honeyrest-domain honeyrest-domain
COPY src src

# Gradle 캐시(~/.gradle)는 BuildKit 캐시 마운트로 빌드 간에 재사용한다 (이미지에는 남지 않음)
RUN --mount=type=cache,target=/root/.gradle,id=honeyrest-gradle \
    ./gradlew --no-daemon bootJar -x test \
 && find build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \; \
 && test -s /workspace/app.jar

# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre AS runtime

# 업로드 볼륨(/app/uploads)을 호스트 관리자 컨테이너와 공유하므로 두 이미지가 같은 UID/GID 를 쓴다.
# (Ubuntu 기반 이미지에 이미 있는 ubuntu(1000) 계정과 겹치지 않는 값)
ARG APP_UID=10001
RUN groupadd --system --gid ${APP_UID} honeyrest \
 && useradd --system --uid ${APP_UID} --gid honeyrest --home-dir /app --shell /usr/sbin/nologin honeyrest \
 && mkdir -p /app/uploads \
 && chown -R honeyrest:honeyrest /app

WORKDIR /app
COPY --from=build --chown=honeyrest:honeyrest /workspace/app.jar /app/app.jar

ENV TZ=Asia/Seoul \
    SPRING_PROFILES_ACTIVE=prod \
    APP_STORAGE_LOCAL_DIR=/app/uploads \
    JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"

USER honeyrest
EXPOSE 8080

# exec: java 가 PID 1 이 되어 docker stop 의 SIGTERM 을 받고 graceful shutdown 한다
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
