# Multi-stage build: compile with Maven, run with a slim JDK.
# Matches docker-compose.yml: build context is ./api

# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*

# Cache Maven dependencies before copying source
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy source and build the fat jar, skipping tests (tests run in CI, not in the image build)
COPY src src
RUN ./mvnw package -DskipTests -B

# --- Runtime stage (distroless: no shell, no package manager) ---
FROM gcr.io/distroless/java21-debian12
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
