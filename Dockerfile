FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/pelycan-api-*.jar app.jar
EXPOSE 4000
ENV PORT=4000
ENTRYPOINT ["java", "-jar", "app.jar"]
