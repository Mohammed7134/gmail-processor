<<<<<<< HEAD
# Use official OpenJDK image with JDK 17 (or 11 if needed)
FROM openjdk:17-jdk-slim

# Set working directory inside the container
=======
# Use official OpenJDK image
FROM openjdk:17-jdk-slim

>>>>>>> 8ac2776 (Initial fresh commit)
WORKDIR /app

# Copy Gradle wrapper and project files
COPY gradlew .
COPY gradle/ gradle/
<<<<<<< HEAD
COPY build.gradle ./
COPY src/ src/

# Make Gradle wrapper executable
RUN chmod +x ./gradlew

# Build the project (creates a jar in build/libs/)
RUN ./gradlew clean build --no-daemon

# Expose port if needed (not strictly necessary for a CLI app)
EXPOSE 8080

# Set environment variable for Gmail credentials (optional default)
# ENV GMAIL_CREDENTIALS_BASE64=""

# Command to run your app
=======
COPY build.gradle settings.gradle ./
COPY src/ src/

# Make gradlew executable
RUN chmod +x gradlew

# Build using the Gradle wrapper (this downloads Gradle automatically)
RUN ./gradlew clean build --no-daemon

# Run the JAR file
>>>>>>> 8ac2776 (Initial fresh commit)
CMD ["java", "-jar", "build/libs/app.jar"]
