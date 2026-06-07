FROM eclipse-temurin:25-jre-alpine AS build

ARG JAR_FILE
WORKDIR /build

ADD $JAR_FILE application.jar
RUN java -Djarmode=layertools -jar application.jar extract --destination extracted

FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache gcompat
RUN addgroup -g 1000 spring-boot-group && \
    adduser -D -u 1000 -G spring-boot-group spring-boot

USER spring-boot:spring-boot-group
VOLUME /tmp
WORKDIR /application

# в том же порядке в котором они описаны в layers.idx
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} org.springframework.boot.loader.launch.JarLauncher $0 $@"]
