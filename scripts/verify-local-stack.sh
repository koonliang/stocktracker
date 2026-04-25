#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
wait_script="$root_dir/scripts/wait-for-http.sh"

backend_health_url="${BACKEND_HEALTH_URL:-http://localhost:8080/q/health}"
backend_api_url="${BACKEND_API_URL:-http://localhost:8080/api}"
frontend_url="${FRONTEND_URL:-http://localhost:5173}"

"$wait_script" "$backend_health_url" 120
"$wait_script" "$frontend_url" 120

backend_health="$(curl --silent --show-error --fail "$backend_health_url")"
dashboard_payload="$(curl --silent --show-error --fail "$backend_api_url/dashboard")"
watchlists_payload="$(curl --silent --show-error --fail "$backend_api_url/watchlists")"

echo "backend health: ok"
echo "$backend_health"
echo
echo "dashboard endpoint: ok"
echo "$dashboard_payload"
echo
echo "watchlists endpoint: ok"
echo "$watchlists_payload"
