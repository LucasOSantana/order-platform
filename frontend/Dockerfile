# syntax=docker/dockerfile:1

# ---------- Build ----------
FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .
RUN npx ng build --configuration production

# ---------- Runtime ----------
FROM nginx:1.27-alpine

# Templates em /etc/nginx/templates passam por envsubst na inicialização
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

ENV BACKEND_URL=http://backend:8080
EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=5 \
    CMD wget -qO- http://127.0.0.1/healthz || exit 1
