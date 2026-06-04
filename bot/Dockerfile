FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY target/*-exec.jar app.jar
EXPOSE 8080 8011
ENTRYPOINT ["java", "-jar", "app.jar"]
