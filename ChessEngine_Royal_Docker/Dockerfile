# ════════════════════════════════════════════════════════
#  Chess Engine Royal — Multi-Stage Dockerfile
#  Stage 1 : Build Java source → compiled .class files
#  Stage 2 : Runtime image — Nginx (HTML) + JRE (server)
# ════════════════════════════════════════════════════════

# ── Stage 1: Build Java ──────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy source tree
COPY src/ ./src/
COPY compile.sh ./compile.sh

# Compile all Java sources
RUN apk add --no-cache bash \
 && chmod +x compile.sh \
 && ./compile.sh

# ── Stage 2: Runtime ─────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# Install Nginx + supervisor (to run both nginx & java server)
RUN apk add --no-cache nginx supervisor bash curl

# ── Web root ─────────────────────────────────────────────
RUN mkdir -p /var/www/chess
COPY chess.html /var/www/chess/chess.html

# ── Compiled Java from builder ───────────────────────────
COPY --from=builder /build/out /app/out

# ── Nginx config ─────────────────────────────────────────
COPY docker/nginx.conf /etc/nginx/nginx.conf
COPY docker/chess-site.conf /etc/nginx/http.d/chess.conf

# ── Supervisor config (manages nginx + java) ─────────────
COPY docker/supervisord.conf /etc/supervisord.conf

# ── Entrypoint ───────────────────────────────────────────
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Ports: 80 = HTTP (chess UI), 9999 = Chess TCP server
EXPOSE 80 9999

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost/chess.html || exit 1

ENTRYPOINT ["/entrypoint.sh"]
