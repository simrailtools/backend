FROM --platform=$BUILDPLATFORM azul/zulu-openjdk-debian:25-latest AS builder
WORKDIR /build

COPY ./ ./
RUN ./gradlew clean :rest-api:buildForDocker -Pversion.release=true -x test -x checkstyleMain -x checkstyleTest --no-daemon

FROM azul/zulu-openjdk-alpine:25-jre-headless-latest
WORKDIR /app

COPY --from=builder /build/rest-api/build/libs/docker/rest-api.jar ./
CMD ["java", "-Duser.timezone=UTC", "-Dspring.profiles.active=common,prd", "-Xms512M", "-XX:+UseZGC", "-XX:+PerfDisableSharedMem", "-XX:+UseCompactObjectHeaders", "--enable-preview", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow", "-jar", "/app/rest-api.jar"]
