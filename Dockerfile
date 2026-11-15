# Una sola imagen con la API y la interfaz: la interfaz Angular compilada se sirve desde Spring Boot.
# El código del frontend llega como contexto adicional «frontend» (ver docker-compose.yml).

FROM node:24-alpine AS interfaz
WORKDIR /app
COPY --from=frontend package.json package-lock.json ./
RUN npm ci
COPY --from=frontend . .
RUN npm run build

FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src/main src/main
COPY --from=interfaz /app/dist/syllabus-frontend/browser src/main/resources/static
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 syllabus && mkdir -p /app/uploads && chown syllabus /app/uploads
COPY --from=build /app/target/syllabus-*.jar app.jar
USER syllabus
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=10 \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
