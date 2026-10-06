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
| infra | — | — | docker-compose: Postgres 16 (5435), Mailpit (8025), Kafka (9092), Kafka UI (8090) |

## Запуск локально

1. `cd infra && docker compose up -d` — Postgres, Mailpit, Kafka, Kafka UI
   (тестовые данные: `docker exec vegas-postgres sh /seed/seed.sh your@email.ru`, см. `infra/seed/README.md`)
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

## События (Kafka)

```
workout-service ──(outbox_events)──► топик workout-events ──► analytics-service
                                       ключ = userId              group-id: analytics-service
                                       заголовок eventType        ошибки -> workout-events.DLT
```

| Событие | Когда | Кто слушает |
|---|---|---|
| `WorkoutCompleted` | тренировка завершена | analytics-service: прогресс, 1ПМ, рекорды |
| `WorkoutDeleted` | удалена завершённая тренировка | analytics-service: убрать из статистики |

Полезные команды:

```bash
# список топиков
docker exec vegas-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
# читать топик с начала (с ключами и заголовками)
docker exec -it vegas-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic workout-events --from-beginning --property print.key=true --property print.headers=true
# состояние consumer group (LAG = сколько сообщений ещё не обработано)
docker exec vegas-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group analytics-service
```

Kafka UI: http://localhost:8090

## Научная база

Формулы и пороги аналитики опираются на исследования — список и привязка к коду: [docs/references.md](docs/references.md).
