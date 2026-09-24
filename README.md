# fitness-journal (Vegas)

Умный дневник тренировок: подходы, прогресс, эталонные кривые, плато, отчёты.

## Модули

| Модуль | Порт | БД | Зона ответственности |
|---|---|---|---|
| common-lib | — | — | общие DTO ошибок, исключения, enum-ы (MuscleGroup, Gender), события |
| gateway-service | 8080 | — | единая точка входа, маршрутизация, (позже) проверка JWT, CORS |
| user-service | 8081 | user_db | регистрация, логин, JWT, OAuth, профиль, история веса |
| workout-service | 8082 | workout_db | каталог упражнений + мышцы + медиа, комплексы, тренировки, подходы |
| analytics-service | 8083 | analytics_db | 1ПМ, прогресс, эталоны, плато, перетрен, разгрузка, рекорды, отчёты |
| notification-service | 8084 | notification_db | email, настройки уведомлений, напоминания |
| frontend | 5173 | — | веб-клиент (позже) |
| infra | — | — | docker-compose: Postgres 16 (5435), Mailpit (8025) |

## Запуск локально

1. `cd infra && docker compose up -d`
2. Открыть корневую папку `fitness-journal` в IntelliJ IDEA как Gradle-проект.
3. Run Configuration сервиса → VM options: `-Dspring.profiles.active=local`
4. Swagger каждого сервиса: `http://localhost:<порт>/swagger-ui.html`

## Структура пакетов сервиса

```
com.vegas.<service>
├── controller   REST
├── service      бизнес-логика
├── repository   Spring Data JPA
├── entity       @Entity
├── dto          запросы/ответы API
├── mapper       MapStruct
├── config       конфигурация
└── exception    обработка ошибок
```
