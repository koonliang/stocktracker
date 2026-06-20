#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
script_path="${repo_root}/scripts/deploy-homelab-sit.sh"

assert_contains() {
  local haystack="$1"
  local needle="$2"
  [[ "$haystack" == *"$needle"* ]] || {
    echo "expected output to contain: $needle" >&2
    echo "$haystack" >&2
    exit 1
  }
}

tmp_env="$(mktemp)"
trap 'rm -f "$tmp_env"' EXIT

cat >"$tmp_env" <<'EOF'
APP_HOST=127.0.0.1
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=stocktracker
DB_USERNAME=tester
DB_PASSWORD=secret
PUBLIC_BASE_URL=https://example.com
DEPLOY_USER=tester
SERVICE_NAME=stocktracker-backend-sit
EOF

success_output="$(
  bash "$script_path" \
    --env-file "$tmp_env" \
    --app-host 127.0.0.2 \
    --validate-only \
    --skip-network-checks
)"
assert_contains "$success_output" "Effective config: profile=sit app_host=127.0.0.2"
assert_contains "$success_output" "Validation complete"

set +e
failure_output="$(
  APP_HOST= \
  DB_HOST=127.0.0.1 \
  DB_PORT=3306 \
  DB_NAME=stocktracker \
  DB_USERNAME=tester \
  DB_PASSWORD=secret \
  PUBLIC_BASE_URL=https://example.com \
  DEPLOY_USER=tester \
  SERVICE_NAME=stocktracker-backend-sit \
  bash "$script_path" --env-file /nonexistent --validate-only --skip-network-checks 2>&1
)"
failure_status=$?
set -e
[[ $failure_status -ne 0 ]] || {
  echo "expected missing APP_HOST validation to fail" >&2
  exit 1
}
assert_contains "$failure_output" "Missing required value: APP_HOST"

echo "deploy-homelab-sit.sh checks passed"
