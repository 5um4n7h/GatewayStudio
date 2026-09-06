# Stage 1: Build the app using JDK 25 + Official Maven binaries
FROM maven:3.9.9-eclipse-temurin-21-alpine AS maven_binaries
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy Maven from the official Maven image
COPY --from=maven_binaries /usr/share/maven /usr/share/maven
COPY --from=maven_binaries /usr/local/bin/mvn-entrypoint.sh /usr/local/bin/mvn-entrypoint.sh
ENV MAVEN_HOME=/usr/share/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

# Cache dependencies
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package application
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Final lightweight runtime container
FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]