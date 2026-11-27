#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

COLLECTION="docs/postman/FuelBridge-API.postman_collection.json"
ENVIRONMENT="docs/postman/FuelBridge-Local.postman_environment.json"
REPORT="docs/postman/last-run-report.json"

# The service-token requests of the collection sign a short-lived JWT with the same shared secret the
# stack uses, so the secret is read before anything is started: from the environment or from .env.
SERVICE_JWT_SECRET="${AUTHORIZATION_JWT_SECRET:-}"
if [[ -z "$SERVICE_JWT_SECRET" && -f .env ]]; then
  SERVICE_JWT_SECRET="$(grep -E '^AUTHORIZATION_JWT_SECRET=' .env | tail -n1 | cut -d= -f2- | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'\$//")"
fi
if [[ -z "$SERVICE_JWT_SECRET" ]]; then
  echo "ERROR: AUTHORIZATION_JWT_SECRET is not set (environment or .env); the service-token requests cannot be signed" >&2
  exit 1
fi

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
NEWMAN_STATUS=0
newman run "$COLLECTION" \
  -e "$ENVIRONMENT" \
  --env-var "serviceJwtSecret=$SERVICE_JWT_SECRET" \
  -r cli,json \
  --reporter-json-export "$REPORT" || NEWMAN_STATUS=$?

# The report is versioned and echoes the environment values, so the secret is scrubbed from it
# (raw and JSON-escaped forms) before anything can commit it, even when newman failed.
if [[ -f "$REPORT" ]]; then
  SECRET_TO_REDACT="$SERVICE_JWT_SECRET" node -e '
    const fs = require("fs");
    const file = process.argv[1];
    const secret = process.env.SECRET_TO_REDACT;
    let text = fs.readFileSync(file, "utf8");
    for (const form of [JSON.stringify(secret).slice(1, -1), secret]) {
      text = text.split(form).join("<redacted>");
    }
    fs.writeFileSync(file, text);
  ' "$REPORT"
fi

echo "==> Report saved to $REPORT"
exit "$NEWMAN_STATUS"
