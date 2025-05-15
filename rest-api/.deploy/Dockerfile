FROM --platform=$BUILDPLATFORM azul/zulu-openjdk:24 AS builder
WORKDIR /build

COPY ./ ./
RUN ./gradlew clean buildForDocker -Pversion.release=true -x test -x checkstyleMain -x checkstyleTest --no-daemon

FROM azul/zulu-openjdk-alpine:24-jre-headless
WORKDIR /app

COPY --from=builder /build/rest-api/build/libs/docker/rest-api.jar ./
CMD ["java", "-Duser.timezone=Europe/Berlin", "-Dspring.profiles.active=common,prd", "-Xms512M", "-XX:+UseZGC", "-jar", "--enable-preview", "/app/rest-api.jar"]
