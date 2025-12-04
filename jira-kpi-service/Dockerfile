# Use Eclipse Temurin JDK 21
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven project files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Build the application
RUN ./mvnw -ntp -DskipTests package

# ---- Runtime Image ----
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port (optional)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
