FROM --platform=$BUILDPLATFORM azul/zulu-openjdk-debian:25-latest AS builder
WORKDIR /build

COPY ./ ./
RUN ./gradlew clean :info-collector:buildForDocker -Pversion.release=true -x test -x checkstyleMain -x checkstyleTest --no-daemon

FROM azul/zulu-openjdk-alpine:25-jre-headless-latest
WORKDIR /app

COPY --from=builder /build/info-collector/build/libs/docker/info-collector.jar ./
CMD ["java", "-Duser.timezone=Europe/Berlin", "-Dspring.profiles.active=common,prd", "-Xms512M", "-XX:+UseZGC", "-XX:+PerfDisableSharedMem", "-XX:+UseCompactObjectHeaders", "-jar", "--enable-preview", "/app/info-collector.jar"]
