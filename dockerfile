# =========================================================
# -------- Stage 1: Build the JAR using Gradle --------
# =========================================================
FROM gradle:8.3-jdk17 AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src/ src/

RUN gradle clean shadowJar --no-daemon

# =========================================================
# -------- Stage 2: Create lightweight runtime image --------
# =========================================================
FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=builder /app/build/libs/*-all.jar app.jar

ENV GMAIL_CREDENTIALS_BASE64=""
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
