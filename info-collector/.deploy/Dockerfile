FROM --platform=$BUILDPLATFORM azul/zulu-openjdk:23 AS builder
WORKDIR /build

COPY ./ ./
RUN ./gradlew clean buildForDocker -Pversion.release=true -x test -x checkstyleMain -x checkstyleTest --no-daemon

FROM azul/zulu-openjdk-alpine:23-jre-headless
WORKDIR /app

COPY --from=builder /build/info-collector/build/libs/docker/info-collector.jar ./
CMD ["java", "-Duser.timezone=Europe/Berlin", "-Dspring.profiles.active=common,prd", "-jar", "--enable-preview", "/app/info-collector.jar"]
