# Multi-stage build: install + build with node, run with node slim.
# Matches docker-compose.yml: build context is ./web

# --- Dependencies stage ---
FROM node:20-alpine AS deps
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci || npm install

# --- Build stage ---
FROM node:20-alpine AS build
WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Build-time env: the API URL as seen from the server during SSR (Docker network)
ENV API_BASE_URL=http://api:8080

RUN npm run build

# --- Runtime stage ---
FROM node:20-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production

# Copy the standalone server output
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public

EXPOSE 3000

CMD ["node", "server.js"]
