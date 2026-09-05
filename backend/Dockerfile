# ============================================================================
# CONFIG MAP (same header in all 5 config files, so any one orients you):
#   .env.example                -> template for local Docker Compose overrides
#   docker-compose.yml          -> builds THIS file for its opt-in backend
#                                   service (--profile full-stack)
#   Dockerfile                  -> YOU ARE HERE: builds a runnable jar image.
#                                   Render also builds straight from this file
#                                   for the real deploy - one recipe, two users.
#   application.properties      -> base+dev config, read when run via `mvnw`
#                                   (not through this file)
#   application-prod.properties -> prod config, read by the app THIS file builds,
#                                   whether run by docker-compose or by Render
# ============================================================================
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first for better caching
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S venuesync && adduser -S venuesync -G venuesync

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Set ownership
RUN chown -R venuesync:venuesync /app
USER venuesync

# Expose port
EXPOSE 8080

# Health check using actuator
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM settings optimized for free-tier (512MB-1GB RAM)
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
