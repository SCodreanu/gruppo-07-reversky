# syntax=docker/dockerfile:1

# ---- Build: compila il jar con il Gradle wrapper del progetto ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Prima solo i file di build, così le dipendenze restano in cache
# finché build.gradle non cambia.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---- Runtime: solo JRE e il jar ----
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /workspace/build/libs/*.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
