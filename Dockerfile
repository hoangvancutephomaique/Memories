# Build API JAR (no -Pprod: GitHub Pages serves the UI separately)
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY backend ./backend
WORKDIR /src/backend
# Use image Maven (not ./mvnw): avoids CRLF/line-ending issues on Linux and missing wrapper jar in some clones.
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /src/backend/target/guestbook-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.jar --server.port=${PORT}"]
