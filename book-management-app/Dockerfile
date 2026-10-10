# =========================
# Build stage
# =========================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests && \
    ls -lh target/


# =========================
# OpenTelemetry Agent stage
# =========================
FROM alpine:3.20 AS otel

ARG OTEL_AGENT_VERSION=1.32.0

RUN apk add --no-cache wget && \
    wget -O /opentelemetry-javaagent.jar \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar" && \
    test -s /opentelemetry-javaagent.jar


# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy OpenTelemetry Java Agent
COPY --from=otel /opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

# Copy application JAR
COPY --from=build /app/target/book-management-app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java","-javaagent:/app/opentelemetry-javaagent.jar","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]