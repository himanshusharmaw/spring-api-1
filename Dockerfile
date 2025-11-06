# Use a supported JDK base image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy built jar
COPY target/*.jar app.jar

# Expose port
EXPOSE 8082

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
