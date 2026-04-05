# Stage 1 — Build the jar
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apt-get update && apt-get install -y maven && \
    mvn -B package -DskipTests

# Stage 2 — Run the jar
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /app/target/dashboard-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]