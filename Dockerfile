FROM node:24-alpine AS frontend-build

WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-25 AS backend-build

WORKDIR /workspace/backend
COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/static
RUN mvn -DskipTests package

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
COPY --from=backend-build /workspace/backend/target/*.jar app.jar

ENV PORT=7860
EXPOSE 7860

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
