# syntax=docker/dockerfile:1
# Фронт: 1) Node собирает статику (npm run build -> dist), 2) Caddy её раздаёт и проксирует /api в gateway.
# Caddy сам получает и продлевает HTTPS-сертификат Let's Encrypt для домена — certbot не нужен.

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM caddy:2-alpine
LABEL org.opencontainers.image.source="https://github.com/veger93/fitness-journal"
LABEL org.opencontainers.image.title="vegas-web"
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist /srv
