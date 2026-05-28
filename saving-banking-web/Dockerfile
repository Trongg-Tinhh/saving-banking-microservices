# ══════════════════════════════════════════════════════════════════
# Stage 1 — Build React app với Vite
# ══════════════════════════════════════════════════════════════════
FROM node:20-alpine AS builder
WORKDIR /app

# Install deps trước (layer cache — chỉ rebuild khi package.json thay đổi)
COPY package.json package-lock.json* ./
RUN npm ci --silent

# Copy toàn bộ source
COPY . .

# VITE_API_BASE_URL='' → axios dùng relative URL (/api/...)
# Nginx trong container sẽ proxy /api → api-gateway nội bộ K8s
ARG  VITE_API_BASE_URL=""
ENV  VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ══════════════════════════════════════════════════════════════════
# Stage 2 — Serve bằng Nginx (chỉ copy dist, không có node_modules)
# ══════════════════════════════════════════════════════════════════
FROM nginx:alpine

# Static files
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx config (SPA fallback + proxy /api)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
