# ============================================================
#  SDN Routing Simulator — Multi-stage Docker Build
#  Stage 1: Maven build → fat JAR
#  Stage 2: Lean JDK runtime
# ============================================================

# --- Stage 1: Build ---
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy Maven Wrapper and project files
COPY .mvn/ .mvn/
COPY mvnw.cmd mvnw ./
COPY pom.xml ./

# Copy source code and frontend
COPY src/ src/
COPY frontend/ frontend/

# Make mvnw executable and build fat JAR (skip tests)
RUN chmod +x mvnw && ./mvnw package -DskipTests -q

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
# -XX:+UseContainerSupport enables JVM awareness of container memory limits
# -XX:MaxRAMPercentage=75 prevents OOM in constrained containers
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-Djava.awt.headless=true", \
     "-jar", "app.jar"]
