# ─── STAGE 1: BUILD ───────────────────────────────────────────────
# This stage uses Maven + JDK 17 to compile your Java code into a jar
# This image is ~500MB but gets thrown away after building
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml FIRST — Docker caches this layer separately
# If your code changes but pom.xml doesn't,
# Maven won't re-download dependencies → faster rebuilds
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Now copy source code and build
COPY src ./src
RUN mvn package -DskipTests -q
# -DskipTests → don't run tests during Docker build (we run them separately)
# -q → quiet mode, less noise in logs

# ─── STAGE 2: RUN ─────────────────────────────────────────────────
# Only JRE (not full JDK) → smaller, more secure image (~120MB)
# alpine = minimal Linux OS, just what we need
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Security best practice: never run app as root inside container
# If container is compromised, attacker gets limited user, not root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy ONLY the built jar from Stage 1 — nothing else
COPY --from=builder /app/target/discovery-server-0.0.1-SNAPSHOT.jar app.jar

# Document which port this service uses
EXPOSE 8761

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
