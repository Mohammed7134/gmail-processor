# -------- Build Stage --------
FROM gradle:8.3-jdk17 AS builder

# Set working directory
WORKDIR /app

# Copy Gradle build files and source code
COPY build.gradle settings.gradle ./
COPY src/ src/

# Build the project (outputs jar in build/libs/)
RUN gradle clean build --no-daemon

# -------- Runtime Stage --------
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose port if your app serves on a port
EXPOSE 8080

# Run the jar
CMD ["java", "-jar", "app.jar"]
