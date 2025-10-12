# Use official OpenJDK image with JDK 17
FROM openjdk:17-jdk-slim

# Set working directory inside the container
WORKDIR /app

# Copy Gradle wrapper and related files
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
COPY src/ src/

# Make Gradle wrapper executable
RUN chmod +x gradlew

# Build the project (this will create a jar in build/libs/)
RUN ./gradlew clean build --no-daemon

# Expose port if your app serves on a port (optional)
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "build/libs/app.jar"]
