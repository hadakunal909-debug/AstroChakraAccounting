# Multi-stage build: compile with Maven, run on a slim JRE.
# The Java backend cannot run on standard cPanel shared hosting — deploy this image
# to a Java-capable host (Railway / Render / Fly.io / a VPS with Docker).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/accounting-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# Provide SUPABASE_DB_URL / SUPABASE_DB_USER / SUPABASE_DB_PASSWORD / JWT_SECRET / CORS_ORIGINS as env vars.
ENTRYPOINT ["java", "-jar", "app.jar"]
