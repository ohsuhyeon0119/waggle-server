# syntax=docker/dockerfile:1

# 1단계: JDK 로 실행 가능한 jar 를 만든다.
# jar 는 CPU 종류와 관계없이 같으므로, 빌드는 이미지를 만드는 컴퓨터의 CPU($BUILDPLATFORM)에서 그대로 한다.
# x86 컴퓨터에서 arm64 이미지를 만들 때도 이 단계는 에뮬레이션 없이 돈다.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 정의만 먼저 복사한다. 소스만 바뀌면 이 레이어는 캐시에서 재사용된다.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies > /dev/null

# 테스트는 Testcontainers 가 도커를 필요로 하므로 이미지 빌드에서는 돌리지 않는다.
COPY src src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test \
    && cp build/libs/*-SNAPSHOT.jar app.jar

# 2단계: 실행에는 JRE 와 jar 만 있으면 된다. 이 단계가 --platform 으로 지정한 CPU 용으로 만들어진다.
FROM eclipse-temurin:21-jre
WORKDIR /app

# root 가 아닌 사용자로 실행한다.
USER 1000:1000

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
