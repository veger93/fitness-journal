#!/usr/bin/env bash
# Кто и как пользуется Vegas — только смотрит, ничего не меняет (на сервере, под root):
#   bash /home/deploy/vegas/activity.sh                 — все пользователи: сколько тренировок и когда последняя
#   bash /home/deploy/vegas/activity.sh mail@example.ru — один пользователь: тренировки по датам + по неделям
# Время — по часовому поясу TZ_SHOW (по умолчанию Челябинск): TZ_SHOW=Europe/Moscow bash activity.sh ...
#
# Показываем только даты, длительность и объём — без весов и упражнений: этого хватает,
# чтобы понять, пользуется ли человек. Демо-аккаунты не показываем.
# Пользователи и тренировки — в разных базах (user_db и workout_db), поэтому список пользователей
# сначала выгружаем из user_db и подаём во временную таблицу в workout_db.

set -euo pipefail
cd "$(dirname "$0")"
DB_USER=$(grep '^DB_USER=' .env | cut -d= -f2)
TZ_SHOW=${TZ_SHOW:-Asia/Yekaterinburg}

# psql в контейнере базы; -X — без чужих настроек, ON_ERROR_STOP — при ошибке сразу выходим
psql_db() { docker compose exec -T postgres psql -X -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$@"; }

# настоящие пользователи (без демо) в CSV: id, кто, когда зарегистрировался (--csv сам экранирует запятые и кавычки)
users_csv() {
  psql_db user_db --csv -t -c "
    SELECT id, coalesce(email, display_name, 'без имени'), created_at
    FROM users
    WHERE auth_provider <> 'DEMO'"
}

if [ $# -eq 0 ]; then
  {
    echo "CREATE TEMP TABLE u (id uuid, who text, registered timestamptz);"
    echo "COPY u FROM STDIN WITH (FORMAT csv);"
    users_csv
    echo '\.'
    cat <<SQL
SELECT u.who                                                     AS "пользователь",
       to_char(u.registered AT TIME ZONE '$TZ_SHOW', 'DD.MM.YYYY') AS "с нами с",
       count(w.id) FILTER (WHERE w.status = 'COMPLETED')         AS "тренировок",
       count(w.id) FILTER (WHERE w.status = 'COMPLETED'
                             AND w.started_at > now() - interval '30 days') AS "за 30 дней",
       coalesce(to_char(max(coalesce(w.started_at, w.created_at)) AT TIME ZONE '$TZ_SHOW', 'DD.MM.YYYY HH24:MI'), '—')
                                                                 AS "последняя активность"
FROM u
LEFT JOIN workouts w ON w.user_id = u.id
GROUP BY u.who, u.registered
ORDER BY max(coalesce(w.started_at, w.created_at)) DESC NULLS LAST;
SQL
  } | psql_db workout_db -q
  exit 0
fi

# один пользователь: адрес передаём переменной psql (:'email') — сам psql экранирует кавычки,
# поэтому никакой адрес не превратится в SQL-команду
USER_ID=$(psql_db user_db -At -v email="$1" <<'SQL'
SELECT id FROM users WHERE lower(email) = lower(:'email');
SQL
)
if [ -z "$USER_ID" ]; then
  echo "Пользователь с адресом $1 не найден" >&2
  exit 1
fi

psql_db workout_db -v uid="$USER_ID" -v tz="$TZ_SHOW" <<'SQL'
\echo '== Тренировки (последние 50) =='
-- started_at пуст у подготовленной, но не начатой тренировки — тогда показываем, когда её создали
SELECT to_char(coalesce(w.started_at, w.created_at) AT TIME ZONE :'tz', 'DD.MM.YYYY HH24:MI') AS "когда",
       CASE WHEN w.status = 'COMPLETED'
            THEN round(extract(epoch FROM w.completed_at - w.started_at) / 60) || ' мин'
            WHEN w.started_at IS NULL THEN 'подготовлена'
            ELSE 'идёт / брошена' END                                                    AS "длилась",
       w.name                                                                           AS "тренировка",
       count(DISTINCT we.id)                                                            AS "упражнений",
       count(s.id)                                                                      AS "подходов"
FROM workouts w
LEFT JOIN workout_exercises we ON we.workout_id = w.id
LEFT JOIN workout_sets s       ON s.workout_exercise_id = we.id
WHERE w.user_id = :'uid'
GROUP BY w.id
ORDER BY coalesce(w.started_at, w.created_at) DESC
LIMIT 50;

\echo '== По неделям (последние 12) =='
SELECT to_char(date_trunc('week', w.started_at AT TIME ZONE :'tz'), 'DD.MM.YYYY') AS "неделя с",
       count(*)                                                                    AS "тренировок"
FROM workouts w
WHERE w.user_id = :'uid' AND w.status = 'COMPLETED'
  AND w.started_at > now() - interval '12 weeks'
GROUP BY 1, date_trunc('week', w.started_at AT TIME ZONE :'tz')
ORDER BY date_trunc('week', w.started_at AT TIME ZONE :'tz') DESC;
SQL
