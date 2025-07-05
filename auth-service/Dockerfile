# --- STAGE 1: BUILDER ---
# Use the latest Maven image which should come with a recent OpenJDK.
# If 'maven:latest' causes issues, you can try 'maven:3-openjdk-24' if it becomes available,
# or even 'openjdk:24-jdk' and then manually install Maven in this stage.
FROM maven:latest AS builder

# Set the working directory inside the container for the build process
WORKDIR /app

# Copy the Maven project files (pom.xml, src/) into the container
# This leverages Docker's build cache: if pom.xml doesn't change,
# the dependencies won't be re-downloaded.
COPY pom.xml .
COPY src ./src

# Package the Spring Boot application into an executable JAR
# -DskipTests is used to skip running tests during the Docker build,
# as tests are usually run in a separate CI/CD stage.
RUN mvn clean package -DskipTests

# --- STAGE 2: RUNNER ---
# Use a slim OpenJDK 24 JRE image for the final, production-ready image.
# 'openjdk:24-jdk-slim' is a good choice for a smaller image.
FROM openjdk:24-jdk-slim

# Set the working directory for the final application
WORKDIR /app

# Copy the built JAR file from the 'builder' stage into this final image.
# The JAR is copied from the 'target/' directory (standard Maven output).
COPY --from=builder /app/target/auth-service-1.0.0.jar auth-service.jar

# Expose the port your Auth Service will listen on
EXPOSE 8081

# Define the command to run your Spring Boot application when the container starts.
ENTRYPOINT ["java", "-jar", "auth-service.jar"]

# Optional: Add a health check if your Spring Boot app has an actuator endpoint
# This requires Spring Boot Actuator dependency in your auth-service's pom.xml
# and potentially installing 'curl' in the runner image if it's not present.
# RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
# HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 CMD curl --fail http://localhost:8081/actuator/health || exit 1