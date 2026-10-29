#!/usr/bin/env bash
# Состояние сервера Vegas одной командой (на сервере, под root):
#   bash /home/deploy/vegas/status.sh
# Ничего не меняет — только смотрит. Деплой кладёт свежую версию файла рядом с docker-compose.yml.

cd "$(dirname "$0")" || exit 1
title() { printf '\n\033[1m== %s ==\033[0m\n' "$1"; }
DOMAIN=$(grep '^DOMAIN=' .env 2>/dev/null | cut -d= -f2)

title "Сервер"
echo "работает: $(uptime -p)"
echo "нагрузка за 1/5/15 мин: $(cut -d' ' -f1-3 /proc/loadavg) (ядер: $(nproc); больше числа ядер — сервер не успевает)"
[ -f /var/run/reboot-required ] && echo "!! после обновлений нужна перезагрузка: reboot"
echo "обновлений системы ждёт: $(apt list --upgradable 2>/dev/null | tail -n +2 | wc -l)"

title "Память и диск"
free -h | head -3
df -h / | awk 'NR==2 {print "диск: занято " $3 " из " $2 " (" $5 ")"}'

title "Сайт"
if [ -n "$DOMAIN" ]; then
  curl -s --max-time 10 -o /dev/null -w "https://$DOMAIN — ответ %{http_code} за %{time_total} с\n" "https://$DOMAIN/" \
    || echo "!! сайт не отвечает"
fi

title "Контейнеры"
docker compose ps --format 'table {{.Service}}\t{{.Status}}'

title "Память и CPU по контейнерам"
docker stats --no-stream --format 'table {{.Name}}\t{{.MemUsage}}\t{{.CPUPerc}}'

title "Ошибки в логах за последний час"
errors=$(docker compose logs --since=1h --no-color 2>/dev/null | grep -E ' ERROR ')
echo "всего: $(printf '%s' "$errors" | grep -c .)"
printf '%s\n' "$errors" | tail -n 3 | cut -c1-220

title "Бэкапы базы"
ls -lht backups 2>/dev/null | sed -n '2,4p' || echo "!! папки backups нет"

title "Попытки подобрать пароль к SSH (сегодня)"
grep "$(date '+%b %e')\|$(date '+%Y-%m-%d')" /var/log/auth.log 2>/dev/null | grep -cE 'Failed|Invalid user'
