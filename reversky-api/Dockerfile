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
# Cartella delle immagini caricate (API-14): il volume di compose.yaml la copre e prende questo proprietario
RUN mkdir -p /app/uploads && chown spring:spring /app/uploads
COPY --from=build /workspace/build/libs/*.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
