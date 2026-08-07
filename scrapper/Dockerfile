FROM maven:3.9.16-eclipse-temurin-25 AS builder

ARG APP_VERSION
ARG BUILD_DATE

LABEL version=${APP_VERSION}
LABEL build_date=${BUILD_DATE}
LABEL maintainer="LinkTracker"

WORKDIR /app

COPY pom.xml ./pom.xml
COPY scrapper/pom.xml ./scrapper/pom.xml

WORKDIR /app/scrapper

RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B \
    -Dmaven.wagon.http.retryHandler.count=3

COPY scrapper/src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -Dmaven.test.skip=true -B

FROM eclipse-temurin:25-jre-alpine

ARG APP_VERSION
ARG BUILD_DATE

LABEL version=${APP_VERSION}
LABEL build_date=${BUILD_DATE}
LABEL maintainer="LinkTracker"

WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/scrapper/target/*.jar app.jar

EXPOSE 8081

USER spring:spring

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
        CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
