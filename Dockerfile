# ── Stage 1: Build Angular frontend ─────────────────────────
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci --silent
COPY frontend/ .
RUN npm run build

# ── Stage 2: Build Spring Boot backend ───────────────────────
FROM eclipse-temurin:21-jdk-alpine AS backend-build
WORKDIR /app/backend
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
RUN ./mvnw dependency:go-offline -q
COPY backend/src src
# Copia il dist di Angular dentro le risorse statiche di Spring Boot
COPY --from=frontend-build /app/frontend/dist/frontend/browser src/main/resources/static/
RUN ./mvnw package -DskipTests -q

# ── Stage 3: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
