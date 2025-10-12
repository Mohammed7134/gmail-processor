# =========================================================
# -------- Stage 1: Build the JAR using Gradle --------
# =========================================================
FROM gradle:8.3-jdk17 AS builder

# Set working directory
WORKDIR /app

# Copy Gradle build files first (to leverage caching)
COPY build.gradle settings.gradle ./

# Copy source code
COPY src/ src/

# Build the project (produces a jar in build/libs/)
RUN gradle clean build --no-daemon

# =========================================================
# -------- Stage 2: Create lightweight runtime image --------
# =========================================================
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# ---------------------------------------------------------
# Add environment variable for Gmail credentials
# (Render will inject this securely from Dashboard > Environment)
# ---------------------------------------------------------
ENV GMAIL_CREDENTIALS_BASE64=""

# Expose port (only needed if your app listens on HTTP)
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "app.jar"]
