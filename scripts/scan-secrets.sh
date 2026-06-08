#!/usr/bin/env bash
# Scan the FULL git history for committed plaintext secrets (FR-026a, SC-004).
# `gitleaks detect` walks every commit, not just the PR diff, so a secret
# introduced anywhere in history fails the required `gates` check. Runnable
# locally and in CI. Installs a pinned gitleaks if it is not on PATH.
set -euo pipefail

GITLEAKS_VERSION="8.18.4"

if command -v gitleaks >/dev/null 2>&1; then
  gitleaks_bin="$(command -v gitleaks)"
else
  echo "gitleaks not found on PATH; downloading v${GITLEAKS_VERSION}..."
  tmp="$(mktemp -d)"
  os="$(uname -s | tr '[:upper:]' '[:lower:]')"
  arch="$(uname -m)"
  case "$arch" in
    x86_64 | amd64) arch="x64" ;;
    aarch64 | arm64) arch="arm64" ;;
  esac
  url="https://github.com/gitleaks/gitleaks/releases/download/v${GITLEAKS_VERSION}/gitleaks_${GITLEAKS_VERSION}_${os}_${arch}.tar.gz"
  curl -fsSL "$url" -o "$tmp/gitleaks.tar.gz"
  tar -xzf "$tmp/gitleaks.tar.gz" -C "$tmp"
  gitleaks_bin="$tmp/gitleaks"
fi

echo "Scanning full git history for secrets..."
"$gitleaks_bin" detect --source . --config .gitleaks.toml --redact --no-banner --exit-code 1
echo "No secrets detected in git history."
