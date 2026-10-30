# syntax=docker/dockerfile:1
# Один Dockerfile на все Java-сервисы: какой собрать — передаём аргументом.
#   docker build --build-arg SERVICE=user-service -t vegas-user-service .
#
# Двухэтапная сборка (multi-stage):
#   1) build — образ с JDK и Gradle собирает jar (весь этот "мусор" в итоговый образ не попадёт);
#   2) runtime — только JRE и готовое приложение: образ меньше и в нём нет компилятора и исходников.
#
# Приложение кладём не одним jar, а слоями (Spring Boot layered jar): библиотеки (~80 МБ) — отдельным слоем,
# наш код (~0,5 МБ) — отдельным. Библиотеки меняются редко, и при выкладке сервер скачивает только наш код.
# Одним jar каждый релиз был бы новым слоем на 80 МБ × 6 сервисов — и выкладка упиралась в таймаут.

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
# раскладываем jar по слоям: dependencies, spring-boot-loader, snapshot-dependencies, application.
# touch: одно и то же время у всех файлов и папок — иначе у слоя с теми же библиотеками
# будет другой хеш (время папок попадает в слой), и Docker посчитает его новым
WORKDIR /extracted
RUN java -Djarmode=tools -jar /app.jar extract --layers --launcher --destination . \
    && find . -exec touch -h -d @0 {} +

FROM eclipse-temurin:17-jre
ARG SERVICE
# привязка образа к репозиторию: в GitHub он появится в разделе Packages рядом с кодом
LABEL org.opencontainers.image.source="https://github.com/veger93/fitness-journal"
LABEL org.opencontainers.image.title="vegas-${SERVICE}"
# запускаем не от root: если в приложении найдут дыру, у взломщика будут права обычного пользователя
RUN useradd --system --uid 1001 app
WORKDIR /app
# от редко меняющихся к часто меняющимся: слой, который не изменился, Docker и сервер берут из кэша
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER app
# MaxRAMPercentage: куча JVM — 75% от лимита памяти контейнера (лимит задаём в docker-compose).
# JarLauncher — тот же запуск, что и у java -jar, только из распакованных папок
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
