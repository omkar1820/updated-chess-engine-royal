#!/bin/sh
# ════════════════════════════════════════════════════════
#  Chess Engine Royal — Container Entrypoint
# ════════════════════════════════════════════════════════
set -e

# Default: don't start Java TCP server unless explicitly enabled
export START_JAVA_SERVER="${START_JAVA_SERVER:-false}"

echo "============================================"
echo "  ♟  Chess Engine Royal — Starting Up"
echo "============================================"
echo "  Nginx (HTTP):        port 80"
if [ "$START_JAVA_SERVER" = "true" ]; then
  echo "  Java Chess Server:   port 9999"
else
  echo "  Java Chess Server:   disabled (set START_JAVA_SERVER=true to enable)"
fi
echo "============================================"

# Ensure nginx run dir exists (Alpine quirk)
mkdir -p /var/run/nginx
mkdir -p /var/log/nginx

exec /usr/bin/supervisord -c /etc/supervisord.conf
