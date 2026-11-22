# syntax=docker/dockerfile:1

# ---- Dipendenze: solo package.json e lockfile, così restano in cache finché non cambiano ----
FROM node:24-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# ---- Build: output "standalone" di Next (next.config.ts), con solo i moduli usati dal server ----
FROM node:24-alpine AS build
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules node_modules
COPY . .
RUN npm run build

# ---- Runtime: server Node di Next, senza sorgenti né devDependencies ----
FROM node:24-alpine
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0

RUN addgroup -S next && adduser -S next -G next
COPY --from=build /app/public ./public
COPY --from=build --chown=next:next /app/.next/standalone ./
COPY --from=build --chown=next:next /app/.next/static ./.next/static
USER next

EXPOSE 3000
CMD ["node", "server.js"]
