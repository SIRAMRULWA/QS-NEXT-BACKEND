# syntax=docker/dockerfile:1

# ============================================================
# Build stage
# ============================================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Copy the wrapper and POM first so dependency resolution is cached in
# its own layer and only re-runs when the POM actually changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

# Now copy the rest of the source and build the jar. Tests already run
# as their own CI step (see .github/workflows/ci.yml) against real
# Testcontainers infrastructure - repeating them in the image build
# would need Docker-in-Docker for no extra safety, so they're skipped
# here.
COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress package -DskipTests

# ============================================================
# Runtime stage
#
# Alpine, not the Ubuntu-based "-jre" tag: smaller image, and BusyBox's
# wget (used by HEALTHCHECK below) ships in it by default, where the
# Ubuntu-based tag has neither wget nor curl preinstalled.
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Never run the application as root inside the container.
RUN addgroup -S qsnext && adduser -S -G qsnext -h /app qsnext
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar
RUN chown -R qsnext:qsnext /app
USER qsnext

EXPOSE 8080

# Reads SERVER_PORT so this stays correct wherever the app actually ends
# up listening (e.g. Render's Docker runtime assigns 10000, overriding
# the 8080 default here and in EXPOSE above) - shell form, so the env
# var is expanded fresh on every check, not baked in at build time.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT:-8080}/actuator/health/liveness || exit 1

# JAVA_TOOL_OPTIONS lets an operator tune JVM flags (heap size,
# container-awareness overrides) at deploy time without rebuilding the
# image.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_TOOL_OPTIONS -jar app.jar"]
