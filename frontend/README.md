# frontend

Веб-клиент Vegas: React 19 + TypeScript + Vite. Ходит только в gateway (`/api/*` → `localhost:8080`).

## Запуск

Нужен **Node.js 22** (LTS) — `node -v`.

```bash
cd frontend
npm install        # один раз и после изменения package.json
npm run dev        # http://localhost:5173
```

Должны быть запущены gateway-service и нужные сервисы (для входа — user-service).
Dev-сервер Vite пересылает `/api/*` в gateway, поэтому CORS не нужен.

```bash
npm test           # тесты (Vitest)
npm run build      # проверка типов + сборка в dist/
```

## Структура

| Папка | Что внутри |
|---|---|
| `src/styles` | `tokens.css` — все цвета/шрифты/скругления двух тем; `global.css` — базовые стили |
| `src/components/ui` | базовые компоненты: Button, Card, TextField, SegmentedControl. Экраны собираем только из них |
| `src/components` | общая раскладка (шапка, навигация) |
| `src/pages` | экраны |
| `src/api` | `client.ts` — fetch + JWT + ошибки бэкенда; по файлу на сервис |
| `src/auth` | вход/выход, хранение токена, защита страниц |
| `src/theme` | светлая / тёмная / как в системе |

Правила стиля: [docs/design/STYLEGUIDE.md](../docs/design/STYLEGUIDE.md).
