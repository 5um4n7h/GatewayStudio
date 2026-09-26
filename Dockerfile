# Stage 1: Maven dependency cache layer (changes rarely)
FROM maven:3.9.9-eclipse-temurin-21-alpine AS maven-cache

WORKDIR /app

# Copy ONLY pom.xml first - this layer caches when dependencies haven't changed
COPY backend/pom.xml ./

# Pre-download all dependencies into Docker layer cache
RUN mvn dependency:go-offline -B -q

# Stage 2: Build application (reuses cached dependencies)
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy cached dependencies from previous stage (instant if unchanged)
COPY --from=maven-cache /root/.m2 /root/.m2
COPY --from=maven-cache /app/pom.xml ./

# Copy source code (rebuilt only when code changes)
COPY backend/src ./src

# Build JAR (reuses cached maven dependencies)
RUN mvn clean package -DskipTests -q

# Stage 3: Runtime (lightweight final image with only JRE, not JDK)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
