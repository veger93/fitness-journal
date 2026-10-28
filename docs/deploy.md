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

**Рекомендую Timeweb Cloud, Москва** (оплата российской картой и СБП, своё зеркало Docker Hub,
понятная панель на русском). Альтернативы с похожими ценами: Selectel, Beget, RuVDS.

Сколько памяти нужно — мы измерили, запустив весь стек: в простое ≈ 1,7 ГБ
(4 сервиса по ~270 МБ, gateway 140 МБ, Kafka ~320 МБ, Postgres ~120 МБ).

| Конфигурация | Цена (Timeweb, осень 2026) | Когда |
|---|---|---|
| 2 vCPU / 4 ГБ / 50 ГБ NVMe | ~1200 ₽/мес | старт, мало пользователей; + файл подкачки 2 ГБ (команда ниже) |
| **4 vCPU / 8 ГБ / 80 ГБ NVMe** | **~2000 ₽/мес** | **рекомендую**: с запасом, без возни с нехваткой памяти |

Конфигурацию можно поднять позже в панели одной кнопкой — данные останутся.

При создании сервера выбрать:
1. Регион — Москва.
2. Образ (ОС) — **Ubuntu 24.04**.
3. SSH-ключ — добавить свой публичный ключ (как его сделать — ниже), тогда вход без пароля.
4. Публичный IPv4 — нужен (обычно включён в цену).

### Ключ SSH для себя (на своём компьютере, PowerShell)

```powershell
ssh-keygen -t ed25519 -C "sergey-laptop"     # Enter, Enter, Enter
type $env:USERPROFILE\.ssh\id_ed25519.pub    # это вставить в панель Timeweb при создании сервера
```

### Первая настройка сервера

Подключаемся (IP — из панели Timeweb):

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
  Можно и в Timeweb — тогда домен и сервер в одном кабинете.
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

notification-service шлёт письма (рекорды, напоминания) через SMTP. Проще всего — Яндекс Почта:

1. Завести ящик, например `vegas.app@yandex.ru`.
2. Яндекс ID → Безопасность → **Пароли приложений** → создать пароль для «Почты».
3. В `.env` на сервере: `MAIL_HOST=smtp.yandex.ru`, `MAIL_PORT=587`, `MAIL_USERNAME=vegas.app@yandex.ru`,
   `MAIL_PASSWORD=<пароль приложения>`, `MAIL_FROM=Vegas <vegas.app@yandex.ru>`.

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
| `pull access denied` / таймаут при скачивании образа | на сервере: `docker pull ghcr.io/veger93/vegas-web:latest`; если ghcr.io не открывается из России — написать мне, переложим образы в реестр Timeweb |
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

Скачать к себе (PowerShell): `scp deploy@<IP>:~/vegas/backups/2026-11-01.sql.gz .`

## Проверка на своём компьютере

Весь продакшен-стек можно поднять локально из готовых образов (после первого деплоя):

```bash
cd deploy
cp .env.example .env    # DOMAIN=http://localhost, остальное — любые значения
docker compose up -d    # сайт: http://localhost
```
