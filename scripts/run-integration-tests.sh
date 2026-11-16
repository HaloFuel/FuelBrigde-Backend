#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

COLLECTION="docs/postman/FuelBridge-API.postman_collection.json"
ENVIRONMENT="docs/postman/FuelBridge-Local.postman_environment.json"
REPORT="docs/postman/last-run-report.json"

POLL_INTERVAL=5
POLL_TIMEOUT=300

echo "==> Building and starting the full docker-compose stack"
docker compose up -d --build

declare -A APP_SERVICE_PORTS=(
  [platform]=8080
  [catalog]=8081
  [notification]=8082
  [reporting]=8083
  [equipment]=8084
  [inventory]=8085
  [payment]=8086
  [fulfillment]=8087
)

wait_for_mysql_healthy() {
  local container="$1"
  local elapsed=0
  echo "==> Waiting for $container to report healthy"
  while true; do
    local status
    status="$(docker inspect --format '{{.State.Health.Status}}' "$container" 2>/dev/null || echo "unknown")"
    if [[ "$status" == "healthy" ]]; then
      echo "    $container is healthy"
      return 0
    fi
    if (( elapsed >= POLL_TIMEOUT )); then
      echo "ERROR: $container did not become healthy within ${POLL_TIMEOUT}s (last status: $status)" >&2
      return 1
    fi
    sleep "$POLL_INTERVAL"
    elapsed=$(( elapsed + POLL_INTERVAL ))
  done
}

wait_for_app_ready() {
  local name="$1"
  local port="$2"
  local elapsed=0
  echo "==> Waiting for $name (port $port) to accept requests"
  while true; do
    if curl -s -o /dev/null -w '%{http_code}' "http://localhost:${port}/api-docs" 2>/dev/null | grep -q '^200$'; then
      echo "    $name is ready"
      return 0
    fi
    if curl -s -o /dev/null -w '%{http_code}' "http://localhost:${port}/v3/api-docs" 2>/dev/null | grep -q '^200$'; then
      echo "    $name is ready"
      return 0
    fi
    if (( elapsed >= POLL_TIMEOUT )); then
      echo "ERROR: $name did not become ready within ${POLL_TIMEOUT}s" >&2
      return 1
    fi
    sleep "$POLL_INTERVAL"
    elapsed=$(( elapsed + POLL_INTERVAL ))
  done
}

MYSQL_CONTAINER="$(docker compose ps -q mysql)"
wait_for_mysql_healthy "$MYSQL_CONTAINER"

for name in "${!APP_SERVICE_PORTS[@]}"; do
  wait_for_app_ready "$name" "${APP_SERVICE_PORTS[$name]}"
done

echo "==> All 9 containers are up and responding"

if ! command -v newman >/dev/null 2>&1; then
  echo "==> newman not found, installing globally"
  npm install -g newman
fi

echo "==> Running the Postman collection against the live stack"
newman run "$COLLECTION" \
  -e "$ENVIRONMENT" \
  -r cli,json \
  --reporter-json-export "$REPORT"

echo "==> Report saved to $REPORT"
