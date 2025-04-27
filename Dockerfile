# Use a lightweight OpenJDK image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy your jar file into the container
COPY target/*.jar app.jar

# Expose port (use your Spring Boot port, usually 8080)
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
