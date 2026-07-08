# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Use the Aliyun Maven mirror (Maven Central is slow from CN networks)
COPY settings.xml /root/.m2/settings.xml
# Copy only pom.xml first to leverage Docker layer caching for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# Create non-root user for security (base image already uses UID 1000,
# so let useradd pick the next free UID instead of forcing 1000)
RUN useradd -m appuser && chown appuser /app
USER appuser
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
