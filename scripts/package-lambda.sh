#!/usr/bin/env bash
# Build the Quarkus backend as an AWS Lambda deployment package.
#
# Output: backend/target/function.zip
# Exits non-zero if the artifact is missing after the build.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${repo_root}/backend"

echo "[package-lambda] building Quarkus Lambda package…"
./mvnw -B -Paws-lambda -DskipTests package

artifact="${repo_root}/backend/target/function.zip"

if [[ ! -f "${artifact}" ]]; then
  echo "[package-lambda] ERROR: expected artifact not found: ${artifact}" >&2
  echo "[package-lambda] target/ contents:" >&2
  ls -la "${repo_root}/backend/target" >&2 || true
  exit 1
fi

size_bytes=$(stat -c%s "${artifact}" 2>/dev/null || stat -f%z "${artifact}")
echo "[package-lambda] OK: ${artifact} (${size_bytes} bytes)"
