#!/usr/bin/env bash

set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "usage: $0 <url> [timeout-seconds]" >&2
  exit 2
fi

url="$1"
timeout="${2:-60}"
deadline=$((SECONDS + timeout))

while (( SECONDS < deadline )); do
  if command -v curl >/dev/null 2>&1; then
    if curl --silent --show-error --fail "$url" >/dev/null; then
      exit 0
    fi
  elif command -v wget >/dev/null 2>&1; then
    if wget -qO- "$url" >/dev/null; then
      exit 0
    fi
  else
    echo "curl or wget is required" >&2
    exit 2
  fi
  sleep 1
done

echo "timed out waiting for $url" >&2
exit 1
