# syntax=docker/dockerfile:1
# Один Dockerfile на все Java-сервисы: какой собрать — передаём аргументом.
#   docker build --build-arg SERVICE=user-service -t vegas-user-service .
#
# Двухэтапная сборка (multi-stage):
#   1) build — образ с JDK и Gradle собирает jar (весь этот "мусор" в итоговый образ не попадёт);
#   2) runtime — только JRE и готовый jar: образ меньше и в нём нет компилятора и исходников.

FROM eclipse-temurin:17-jdk AS build
ARG SERVICE
WORKDIR /src
# сначала только файлы сборки: если код поменялся, а они нет — Docker возьмёт слой с зависимостями из кэша
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle gradle
RUN ./gradlew --no-daemon --version
COPY . .
# тесты гоняет CI отдельным шагом, здесь только сборка
RUN ./gradlew --no-daemon :${SERVICE}:bootJar -x test \
    && cp ${SERVICE}/build/libs/${SERVICE}-*-SNAPSHOT.jar /app.jar

FROM eclipse-temurin:17-jre
ARG SERVICE
# привязка образа к репозиторию: в GitHub он появится в разделе Packages рядом с кодом
LABEL org.opencontainers.image.source="https://github.com/veger93/fitness-journal"
LABEL org.opencontainers.image.title="vegas-${SERVICE}"
# запускаем не от root: если в приложении найдут дыру, у взломщика будут права обычного пользователя
RUN useradd --system --uid 1001 app
COPY --from=build /app.jar /app/app.jar
USER app
# MaxRAMPercentage: куча JVM — 75% от лимита памяти контейнера (лимит задаём в docker-compose)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
