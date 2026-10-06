#!/bin/sh
# Наполнение dev-базы тестовыми данными. Запускается ВНУТРИ контейнера vegas-postgres:
#   docker exec vegas-postgres sh /seed/seed.sh your@email.ru
# Аккаунт с этим email сохраняется, всё остальное очищается и создаётся заново.
set -e

EMAIL="$1"
if [ -z "$EMAIL" ]; then
  echo "Использование: sh /seed/seed.sh <email вашего аккаунта>"
  exit 1
fi

PSQL="psql -U vegas -q -v ON_ERROR_STOP=1"

# email передаём как переменную psql (:'email'), а не подстановкой в текст SQL — так нет SQL-инъекции
USER_ID=$(echo "SELECT id FROM users WHERE email = lower(trim(:'email'));" \
  | $PSQL -d user_db -tA -v email="$EMAIL")

if [ -z "$USER_ID" ]; then
  echo "Пользователь $EMAIL не найден в user_db. Сначала зарегистрируйтесь: POST /api/auth/register"
  exit 1
fi
echo "Пользователь: $EMAIL ($USER_ID)"

# порядок важен: сначала очищаем аналитику, потом создаём тренировки (их события заполнят её заново)
$PSQL -d analytics_db -f /seed/03-analytics.sql
$PSQL -d user_db      -v user_id="$USER_ID" -f /seed/01-user.sql
$PSQL -d workout_db   -v user_id="$USER_ID" -f /seed/02-workouts.sql

echo "Готово. Если user-, workout- и analytics-service запущены, статистика пересчитается в течение ~20 секунд."
