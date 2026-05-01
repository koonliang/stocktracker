#!/usr/bin/env bash
# Post-deploy smoke check.
#
# Usage: smoke-check.sh <frontend-url> <api-url>
#
# Asserts:
#   - GET <frontend-url>/        returns 2xx
#   - GET <api-url>/q/health     returns 2xx
#
# Exits non-zero on any non-2xx response or unreachable host.

set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 <frontend-url> <api-url>" >&2
  exit 2
fi

frontend_url="${1%/}"
api_url="${2%/}"

check() {
  local label="$1"
  local url="$2"
  local code

  code=$(curl --silent --show-error --location --max-time 15 \
    --output /dev/null --write-out '%{http_code}' "${url}" || true)

  if [[ "${code}" =~ ^2[0-9][0-9]$ ]]; then
    echo "[smoke] ${label}: ${url} -> ${code} OK"
    return 0
  fi

  echo "[smoke] ${label}: ${url} -> ${code} FAIL" >&2
  return 1
}

failed=0
check "frontend" "${frontend_url}/"        || failed=1
check "api-health" "${api_url}/q/health"   || failed=1

if [[ "${failed}" -ne 0 ]]; then
  echo "[smoke] one or more checks failed" >&2
  exit 1
fi

echo "[smoke] all checks passed"
