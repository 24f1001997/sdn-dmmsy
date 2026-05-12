# ============================================================
#  SDN Routing Simulator — Multi-stage Docker Build
#  Stage 1: Maven build → fat JAR
#  Stage 2: Lean JDK runtime
# ============================================================

# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy POM first for dependency caching
COPY pom.xml ./

# Download dependencies (cached layer unless pom.xml changes)
RUN mvn dependency:go-offline -q

# Copy source code and frontend
COPY src/ src/
COPY frontend/ frontend/

# Build fat JAR (skip tests for faster builds)
RUN mvn package -DskipTests -q

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the fat JAR from builder
COPY --from=builder /app/target/sdn-dmmsy-simulator-1.0.0.jar app.jar

# Create output directory for generated charts
RUN mkdir -p /app/output

# Cloud platforms set PORT env var; default to 8085
ENV PORT=8085

# Expose the port
EXPOSE ${PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:${PORT}/api/status || exit 1

# Run the application
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-Djava.awt.headless=true", \
     "-jar", "app.jar"]
