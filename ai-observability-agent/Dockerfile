FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /app/target/ai-observability-agent.jar /app/app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]



