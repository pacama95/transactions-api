####
# This Dockerfile is used to build a native runnable image of the logo_api application.
# Build stage
####
FROM quay.io/quarkus/ubi-quarkus-graalvmce-builder-image:jdk-21 AS build

# Set working directory
WORKDIR /code

# Copy gradle files
COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle settings.gradle gradle.properties ./
COPY gradle/libs.versions.toml gradle/libs.versions.toml

# Copy lombok configuration
COPY lombok.config ./

# Download dependencies (this layer is cached unless dependencies change)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build the native executable
RUN ./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=false --no-daemon

# Runtime stage - minimal image
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.6

# Create application directory
WORKDIR /app

# Copy the native executable from build stage
COPY --from=build --chown=1001:root /code/build/*-runner ./application

# Set proper permissions
RUN chmod +x ./application && \
    chown 1001:root ./application

# Switch to non-root user
USER 1001

# Expose port (Railway will set the PORT environment variable)
EXPOSE $PORT

# Set production profile for Railway deployment
ENV QUARKUS_PROFILE=prod

# Health check for Railway
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/q/health || exit 1

# Start the application
CMD ["sh", "-c", "./application -Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=${PORT:-8080}"]