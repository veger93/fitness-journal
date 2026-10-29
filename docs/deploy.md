# Выкладка Vegas в интернет — пошагово, с нуля

Этот документ — для первого запуска. Дальше всё происходит само: смержил PR в `main` →
GitHub проверил код → собрал образы → обновил сервер.

## 0. Как всё устроено (картинка словами)

```
 Ты: git push, PR, merge в main
        │
        ▼
 GitHub Actions ── CI: тесты бэкенда и фронта (.github/workflows/ci.yml)
        │  зелёный
        ▼
 GitHub Actions ── Deploy (.github/workflows/deploy.yml):
        │   1. собирает Docker-образы 5 сервисов + фронта
        │   2. кладёт их в GitHub Container Registry (ghcr.io) — "склад образов" при репозитории
        │   3. заходит на сервер по SSH (ключ лежит в Secrets репозитория)
        ▼
 Сервер (VPS) ── docker compose pull && up -d (deploy/docker-compose.yml)
        │   Caddy (web) :443 ──► фронт (статические файлы)
        │                  └──► /api/* ──► gateway ──► 4 сервиса ──► Postgres, Kafka
        ▼
 Домен vegas-fit.ru ──(DNS: A-запись)──► IP сервера
```

Три вещи, которые нужно купить/завести один раз: **сервер (VPS)**, **домен**, **почту для писем**.
GitHub Actions для публичного репозитория — **бесплатно**.

## 1. GitHub Actions: сколько стоит

- Репозиторий **публичный** → стандартные раннеры GitHub (ubuntu-latest) **бесплатны без лимита минут**,
  ghcr.io для публичных образов тоже бесплатен. Платить ничего не нужно.
- Репозиторий **приватный** → бесплатный план даёт 2000 минут в месяц. Наша сборка ≈ 10–15 минут
  (CI + Deploy), то есть ~150 выкладок в месяц — тоже хватит.
- Оплата: GitHub не принимает российские карты, поэтому план **Free** — то, что нужно. Ничего не подключаем.

Проверить, публичный ли репозиторий: Settings → General → в самом низу блок **Danger Zone** →
"Change repository visibility" показывает текущее состояние. Секреты (пароли, ключи) в публичном
репозитории не видны никому — они хранятся отдельно (Secrets), а не в коде.

### Словарик интерфейса GitHub

| На экране | По-русски | Зачем нам |
|---|---|---|
| Settings | Настройки репозитория | всё ниже — отсюда |
| Secrets and variables → Actions | Секреты и переменные для Actions | ключ SSH, адрес сервера |
| New repository secret | Добавить секрет | |
| Variables (вкладка) | Обычные (не секретные) переменные | `DEPLOY_ENABLED` |
| Actions (вкладка вверху) | Запуски проверок и деплоя | смотреть логи, запустить вручную |
| Run workflow | Запустить вручную | первый деплой |
| Re-run jobs | Перезапустить | если упало из-за сети |
| Branches → Add branch ruleset | Правила для ветки | запретить мерж с красным CI |
| Packages (справа на главной репозитория) | Склад образов ghcr.io | там появятся `vegas-*` |

## 2. Сервер (VPS)

Сервер — это арендованный компьютер в дата-центре, на нём круглосуточно работают наши контейнеры.

Провайдер — любой с дата-центром в России, оплатой российской картой/СБП и образом Ubuntu 24.04.
Цены сильно отличаются (данные servercalc.ru на 25.10.2026, перед оплатой сверить на сайте провайдера):

| Провайдер | 4 ГБ | 8 ГБ |
|---|---|---|
| FirstVDS | 2 vCPU / 4 ГБ / 60 ГБ SSD — ~920 ₽ | **4 vCPU / 8 ГБ / 80 ГБ SSD — ~1580 ₽** |
| Beget | 2 vCPU / 4 ГБ — ~1140 ₽ | — |
| VDSina | 2 vCPU / 4 ГБ / 100 ГБ NVMe — ~1200 ₽ | 4 vCPU / 8 ГБ / 150 ГБ NVMe — ~2400 ₽ |
| Timeweb Cloud | 2 vCPU / 4 ГБ — ~3840 ₽ | 4 vCPU / 8 ГБ — ~7000 ₽ |

Сколько памяти нужно — мы измерили, запустив весь стек: в простое ≈ 1,7 ГБ
(4 сервиса по ~270 МБ, gateway 140 МБ, Kafka ~320 МБ, Postgres ~120 МБ).
**8 ГБ — рекомендую** (с запасом, без возни с нехваткой памяти); 4 ГБ — бюджетный старт + файл подкачки 2 ГБ.

Зеркала Docker Hub из шага ниже (`dockerhub.timeweb.cloud`, `mirror.gcr.io`) публичные — работают на сервере
любого провайдера.

Конфигурацию можно поднять позже в панели одной кнопкой — данные останутся.

При создании сервера выбрать:
1. Регион — Москва.
2. Образ (ОС) — **Ubuntu 24.04**.
3. SSH-ключ — добавить свой публичный ключ (как его сделать — ниже), тогда вход без пароля.
4. Публичный IPv4 — нужен (обычно включён в цену).

### Ключ SSH для себя (на своём компьютере, PowerShell)

```powershell
ssh-keygen -t ed25519 -C "sergey-laptop"     # Enter, Enter, Enter
type $env:USERPROFILE\.ssh\id_ed25519.pub    # это вставить в панель провайдера при создании сервера
```

### Первая настройка сервера

Подключаемся (IP — из панели провайдера):

```bash
ssh root@<IP-сервера>
```

Дальше — команды на сервере, по порядку:

```bash
# 1. Обновить систему и поставить Docker из репозитория Ubuntu (работает без VPN)
apt update && apt -y upgrade
apt -y install docker.io docker-compose-v2 ufw

# 2. Зеркала Docker Hub: образы postgres, kafka, caddy будут качаться через них
cat > /etc/docker/daemon.json <<'EOF'
{ "registry-mirrors": ["https://dockerhub.timeweb.cloud", "https://mirror.gcr.io"] }
EOF
systemctl restart docker

# 3. Файрвол: снаружи открыты только SSH и сайт
ufw allow 22/tcp && ufw allow 80/tcp && ufw allow 443/tcp && ufw --force enable

# 4. (только для сервера с 4 ГБ) файл подкачки 2 ГБ — страховка от нехватки памяти
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# 5. Отдельный пользователь для выкладки (под root GitHub на сервер не пускаем)
adduser --disabled-password --gecos "" deploy
usermod -aG docker deploy
mkdir -p /home/deploy/.ssh /home/deploy/vegas
```

### Ключ SSH для GitHub Actions

Отдельный ключ, только для деплоя (если украдут — отзовём, не трогая твой):

```bash
# на сервере, под root
ssh-keygen -t ed25519 -f /root/github-deploy -N "" -C "github-actions"
cat /root/github-deploy.pub >> /home/deploy/.ssh/authorized_keys
chown -R deploy:deploy /home/deploy/.ssh /home/deploy/vegas
chmod 700 /home/deploy/.ssh && chmod 600 /home/deploy/.ssh/authorized_keys
cat /root/github-deploy        # приватный ключ: целиком, с BEGIN/END — в секрет SSH_PRIVATE_KEY
rm /root/github-deploy*        # после того как сохранил в GitHub — копию на сервере удаляем
```

### Файл .env с паролями (на сервере)

```bash
su - deploy
cd ~/vegas
nano .env       # содержимое — по образцу deploy/.env.example из репозитория
```

Пароли сгенерировать там же: `openssl rand -base64 24` (для БД) и `openssl rand -base64 64` (для JWT_SECRET,
одной строкой). `.env` живёт только на сервере — в git его нет.

## 3. Домен

Домен — это адрес сайта (`vegas-fit.ru` — здесь и дальше пример, имя выбираешь сам). Покупается у регистратора на год, потом продлевается.

- Где: **REG.RU** или **RU-CENTER (nic.ru)** — крупнейшие, оплата картой/СБП.
- Сколько: домен `.ru` — порядка 1–1,5 тыс. ₽ в год; в 2026 продление у многих регистраторов
  подорожало (например, у Webnames — до 1390 ₽). Смотри цену **продления**, а не только первого года.
- Для `.ru` нужны паспортные данные владельца (требование регистратора `.ru`).

После покупки — в панели домена → **DNS-записи**:

| Тип | Имя | Значение |
|---|---|---|
| A | `@` | IP сервера |

DNS обновляется от 15 минут до суток. Проверить: `ping vegas-fit.ru` должен показать IP сервера.
HTTPS-сертификат отдельно покупать **не нужно**: Caddy получит бесплатный Let's Encrypt сам при первом запуске,
как только домен начнёт указывать на сервер.

## 4. Почта для писем

notification-service шлёт письма (рекорды, напоминания) через SMTP. У нас — ящик на Mail.ru
(Яндекс просит отдельный номер телефона на каждый ящик, Mail.ru позволил завести второй ящик к тому же аккаунту).

1. Завести ящик (у нас `vegas-fit@mail.ru`).
2. В этом же ящике: **Настройки → Все настройки → Безопасность → Пароли для внешних приложений** →
   создать, тип «Только отправка писем». Пароль показывают один раз.
3. На сервере `nano /home/deploy/vegas/.env` (сохранить: Ctrl+O, Enter; выйти: Ctrl+X):

   ```
   MAIL_HOST=smtp.mail.ru
   MAIL_PORT=587
   MAIL_USERNAME=vegas-fit@mail.ru
   MAIL_PASSWORD=<пароль приложения>
   MAIL_FROM=Vegas <vegas-fit@mail.ru>
   ```

   Порт 587 — потому что сервис включает STARTTLS (465 — другой вид шифрования, с нашими настройками не заработает).
   `MAIL_FROM` — тот же адрес, что `MAIL_USERNAME` (от чужого имени Mail.ru слать не даст), и ровно одна пара `<...>`.
4. `cd /home/deploy/vegas && docker compose up -d` — перезапустится только notification-service.
5. Проверка: Профиль → уведомления → тестовое письмо; логи —
   `docker compose logs --since=10m notification-service | grep -i -E "sent|mail|error"`.

Частые ошибки в логе:
- `AddressException: Extra route-addr` — в `MAIL_FROM` лишний текст (например, остался старый `<you@yandex.ru>`).
- `AuthenticationFailedException` — обычный пароль вместо пароля приложения, пароль от другого ящика или пробел при вставке.
- `Connection timed out` — провайдер закрыл порт; проверить: `timeout 5 bash -c '</dev/tcp/smtp.mail.ru/587' && echo ok`.

Яндекс тоже подходит: `smtp.yandex.ru`, порт 587, пароль приложения из Яндекс ID → Безопасность.

## 5. Соединяем GitHub с сервером

Settings → **Secrets and variables** → **Actions**:

Вкладка **Secrets** → New repository secret (три штуки):

| Name | Value |
|---|---|
| `SSH_HOST` | IP сервера |
| `SSH_USER` | `deploy` |
| `SSH_PRIVATE_KEY` | приватный ключ из шага «Ключ SSH для GitHub Actions», целиком |

Вкладка **Variables** → New repository variable:

| Name | Value |
|---|---|
| `DEPLOY_ENABLED` | `true` |

Пока `DEPLOY_ENABLED` не задан, workflow Deploy просто пропускается — поэтому он не падал раньше.

## 6. Первый запуск

1. Вкладка **Actions** → слева **Deploy** → справа **Run workflow** → Run.
2. Ждём зелёные галочки (~10 минут в первый раз, дальше быстрее за счёт кэша).
3. Открываем `https://vegas-fit.ru` — регистрируемся.

Если упало — открыть красный шаг, внизу будет текст ошибки. Частые случаи:

| Ошибка | Что делать |
|---|---|
| `ssh: handshake failed` / `unable to authenticate` | проверить `SSH_USER`, ключ скопирован целиком |
| `denied: permission_denied` при push в ghcr.io | Settings → Actions → General → Workflow permissions → **Read and write permissions** |
| `pull access denied` / таймаут при скачивании образа | на сервере: `docker pull ghcr.io/veger93/vegas-web:latest`; если ghcr.io не открывается из России — написать мне, переложим образы в российский реестр (например, Yandex Container Registry) |
| сайт открывается без HTTPS / ошибка сертификата | домен ещё не указывает на IP (DNS не обновился) — подождать и `docker compose restart web` |

Полезные команды на сервере (`su - deploy && cd ~/vegas`):

```bash
docker compose ps                       # что запущено
docker compose logs -f user-service     # логи сервиса (Ctrl+C — выйти)
docker compose restart notification-service
```

## 7. Потом: обычная работа

- Новая фича → ветка → PR → CI зелёный → merge → **через ~5 минут она на сайте**, руками ничего не делаем.
- Откат: в `.env` на сервере вписать `TAG=<хеш предыдущего коммита>` и `docker compose up -d`.
  Каждый образ хранится в ghcr.io с тегом-коммитом.
- Запретить мерж с красным CI: Settings → **Rules → Rulesets** → New branch ruleset → ветка `main` →
  включить **Require status checks to pass** → добавить `Backend (Gradle)` и `Frontend (Node)`.

## 8. Резервные копии базы

Минимум — ежедневный дамп на сервере (и время от времени скачивать себе):

```bash
# под deploy: crontab -e и добавить строку — каждый день в 4:00 дамп всех баз, хранится 14 дней
0 4 * * * cd ~/vegas && mkdir -p backups && docker compose exec -T postgres pg_dumpall -U vegas | gzip > backups/$(date +\%F).sql.gz && find backups -mtime +14 -delete
```

Скачать к себе (PowerShell): `scp root@<IP>:/home/deploy/vegas/backups/2026-11-01.sql.gz .`

## 9. Работа с сервером

### Вход по ключу вместо пароля

Пароль root перебирают боты со всего интернета — после настройки ключа вход по паролю выключаем.

```powershell
# на своём компьютере (PowerShell), один раз
ssh-keygen -t ed25519 -C "sergey-pc"            # Enter на все вопросы
type $env:USERPROFILE\.ssh\id_ed25519.pub | ssh root@<IP> "cat >> ~/.ssh/authorized_keys"
ssh root@<IP>                                   # должно пустить БЕЗ пароля
```

Только убедившись, что без пароля пускает, — на сервере:

```bash
echo "PasswordAuthentication no" > /etc/ssh/sshd_config.d/00-no-password.conf
systemctl restart ssh
```

Если ключ потерян — вход через VNC-консоль в панели провайдера (она не зависит от SSH).

### Состояние сервера одной командой

```bash
bash /home/deploy/vegas/status.sh
```

Скрипт `deploy/status.sh` ничего не меняет, только показывает: нагрузку, память и диск, ответ сайта,
состояние контейнеров и их память, ошибки в логах за час, последние бэкапы, попытки подобрать пароль к SSH.
Строки с `!!` — повод разобраться. Деплой кладёт свежую версию скрипта на сервер сам.

### Полезные команды (на сервере)

```bash
cd /home/deploy/vegas
docker compose ps                                   # что запущено, healthy ли
docker compose logs -f --tail=100 workout-service   # логи сервиса (Ctrl+C — выйти)
docker compose logs --since=1h gateway | grep -i error
docker stats --no-stream                            # память и CPU по контейнерам
docker compose restart notification-service         # перезапуск одного сервиса
docker compose up -d                                # применить изменения .env
df -h && free -h                                    # диск и память сервера
```

Графики CPU/трафика и перезагрузка сервера — в панели провайдера.

### DBeaver: база через SSH-туннель

Postgres слушает только `127.0.0.1` сервера — из интернета его не видно. DBeaver заходит на сервер по SSH
и уже оттуда подключается к базе («туннель»).

1. DBeaver → **Новое подключение → PostgreSQL**.
2. Вкладка **Main**: Host `localhost`, Port `5432`, Database `workout_db` (или `user_db`, `analytics_db`,
   `notification_db`), Username / Password — `DB_USER` / `DB_PASSWORD` из `.env` на сервере
   (посмотреть: `grep DB_ /home/deploy/vegas/.env`). Галочка **Show all databases** — все базы в одном подключении.
3. Вкладка **SSH**: Use SSH Tunnel, Host `<IP>`, Port `22`, User `root`, Authentication **Public Key**,
   Private key — `C:\Users\<ты>\.ssh\id_ed25519`.
4. **Test Connection**.

Это продакшен: читать — сколько угодно, менять данные руками — только понимая последствия
(и после свежего бэкапа). Схему меняем только миграциями Liquibase.

## Проверка на своём компьютере

Весь продакшен-стек можно поднять локально из готовых образов (после первого деплоя):

```bash
cd deploy
cp .env.example .env    # DOMAIN=http://localhost, остальное — любые значения
docker compose up -d    # сайт: http://localhost
```
