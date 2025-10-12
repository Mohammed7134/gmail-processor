# Use official OpenJDK 17 image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy Gradle wrapper scripts and wrapper jar
COPY gradlew .
COPY gradle/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.jar
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.properties

# Copy build files first to leverage Docker caching
COPY build.gradle settings.gradle ./

# Copy source code
COPY src/ src/

# Make Gradle wrapper executable
RUN chmod +x gradlew

# Build the project
RUN ./gradlew clean build --no-daemon

# Expose port if needed
EXPOSE 8080

# Run the built JAR (adjust the name if needed)
CMD ["java", "-jar", "build/libs/app.jar"]
