#!/usr/bin/env bash
# Deploy the Quarkus backend to the private homelab SIT environment.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
backend_dir="${repo_root}/backend"

ENV_FILE="${repo_root}/scripts/.env"
QUARKUS_PROFILE="${QUARKUS_PROFILE:-sit}"
ARTIFACT_PATH="${ARTIFACT_PATH:-}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-stocktracker}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/stocktracker/sit}"
REMOTE_LOG_FILE="${REMOTE_LOG_FILE:-/var/log/stocktracker-sit.log}"
SSH_PORT="${SSH_PORT:-22}"
SIT_AUTH_MODE="${SIT_AUTH_MODE:-dev}"
SIT_DEV_BOOTSTRAP_ENABLED="${SIT_DEV_BOOTSTRAP_ENABLED:-true}"
SIT_SCHEDULER_ENABLED="${SIT_SCHEDULER_ENABLED:-true}"
DRY_RUN="false"
VALIDATE_ONLY="false"
SKIP_NETWORK_CHECKS="false"
TEMP_BUNDLE_PATH=""
TEMP_RUNTIME_ENV_PATH=""
TEMP_SYSTEMD_UNIT_PATH=""

log() {
  printf '[homelab-sit] %s\n' "$*"
}

fail() {
  printf '[homelab-sit] ERROR: %s\n' "$*" >&2
  exit 1
}

cleanup_temp_files() {
  rm -f "${TEMP_BUNDLE_PATH:-}" "${TEMP_RUNTIME_ENV_PATH:-}" "${TEMP_SYSTEMD_UNIT_PATH:-}"
}

usage() {
  cat <<'EOF'
Usage: scripts/deploy-homelab-sit.sh [options]

Options:
  --env-file PATH           Load deployment inputs from PATH (default: scripts/.env)
  --app-host HOST           Backend app host / IP
  --db-host HOST            MySQL host / IP
  --db-port PORT            MySQL port (default: 3306)
  --db-name NAME            Database/schema name (default: stocktracker)
  --db-username USER        Database username
  --db-password PASS        Database password
  --public-base-url URL     Public backend base URL used for health verification
  --artifact-path PATH      Prebuilt tar.gz or quarkus-app directory to deploy
  --deploy-user USER        SSH user on the app host
  --service-name NAME       systemd unit name or process label on the app host
  --deploy-dir PATH         Remote deployment directory (default: /opt/stocktracker/sit)
  --remote-log-file PATH    Remote log file when falling back to nohup
  --ssh-port PORT           SSH port (default: 22)
  --dry-run                 Print the planned actions without building or deploying
  --validate-only           Validate inputs and connectivity, then exit
  --skip-network-checks     Skip app/db reachability probes
  --help                    Show this help
EOF
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

set_override() {
  local name="$1"
  local value="$2"
  if [[ -n "$value" ]]; then
    printf -v "$name" '%s' "$value"
    export "$name"
  fi
}

load_env_file() {
  local env_file="$1"
  if [[ -f "$env_file" ]]; then
    log "Loading deployment inputs from $env_file"
    set -a
    # shellcheck source=/dev/null
    source "$env_file"
    set +a
  fi
}

validate_required() {
  local name
  for name in APP_HOST DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD PUBLIC_BASE_URL DEPLOY_USER SERVICE_NAME; do
    [[ -n "${!name:-}" ]] || fail "Missing required value: $name"
  done
}

validate_port() {
  [[ "$1" =~ ^[0-9]+$ ]] || fail "Port must be numeric: $1"
  (( "$1" >= 1 && "$1" <= 65535 )) || fail "Port must be between 1 and 65535: $1"
}

probe_tcp() {
  local host="$1"
  local port="$2"
  local label="$3"
  if [[ "$SKIP_NETWORK_CHECKS" == "true" ]]; then
    log "Skipping ${label} reachability check"
    return 0
  fi

  if command -v nc >/dev/null 2>&1; then
    nc -z -w 5 "$host" "$port" >/dev/null 2>&1 || fail "Cannot reach ${label} at ${host}:${port}"
    return 0
  fi

  if command -v timeout >/dev/null 2>&1; then
    timeout 5 bash -lc ">/dev/tcp/${host}/${port}" >/dev/null 2>&1 || fail "Cannot reach ${label} at ${host}:${port}"
    return 0
  fi

  fail "Network checks require either nc or timeout to be installed"
}

build_bundle() {
  local bundle_path="$1"

  if [[ -n "${ARTIFACT_PATH}" ]]; then
    if [[ -d "${ARTIFACT_PATH}" ]]; then
      tar -C "$(dirname "${ARTIFACT_PATH}")" -czf "$bundle_path" "$(basename "${ARTIFACT_PATH}")"
      log "Packaged prebuilt quarkus-app directory from ${ARTIFACT_PATH}"
      return 0
    fi
    if [[ -f "${ARTIFACT_PATH}" ]]; then
      cp "${ARTIFACT_PATH}" "$bundle_path"
      log "Using prebuilt deployment bundle ${ARTIFACT_PATH}"
      return 0
    fi
    fail "ARTIFACT_PATH does not exist: ${ARTIFACT_PATH}"
  fi

  log "Building backend JVM artifact"
  (
    cd "$backend_dir"
    ./mvnw -B -DskipTests package
  )

  local quarkus_app_dir="${backend_dir}/target/quarkus-app"
  [[ -f "${quarkus_app_dir}/quarkus-run.jar" ]] || fail "Expected Quarkus JVM bundle not found in ${quarkus_app_dir}"

  tar -C "${backend_dir}/target" -czf "$bundle_path" quarkus-app
  log "Created deployment bundle ${bundle_path}"
}

write_runtime_env() {
  local env_output="$1"
  cat >"$env_output" <<EOF
QUARKUS_PROFILE=${QUARKUS_PROFILE}
QUARKUS_DATASOURCE_JDBC_URL=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
QUARKUS_DATASOURCE_USERNAME=${DB_USERNAME}
QUARKUS_DATASOURCE_PASSWORD=${DB_PASSWORD}
SIT_AUTH_MODE=${SIT_AUTH_MODE}
SIT_DEV_BOOTSTRAP_ENABLED=${SIT_DEV_BOOTSTRAP_ENABLED}
SIT_SCHEDULER_ENABLED=${SIT_SCHEDULER_ENABLED}
NONPROD_GOOGLE_CLIENT_ID=${NONPROD_GOOGLE_CLIENT_ID:-}
NONPROD_GOOGLE_CLIENT_SECRET=${NONPROD_GOOGLE_CLIENT_SECRET:-}
NONPROD_FACEBOOK_CLIENT_ID=${NONPROD_FACEBOOK_CLIENT_ID:-}
NONPROD_FACEBOOK_CLIENT_SECRET=${NONPROD_FACEBOOK_CLIENT_SECRET:-}
NONPROD_SOCIAL_REDIRECT_URI=${NONPROD_SOCIAL_REDIRECT_URI:-}
EOF
}

normalize_systemd_unit_name() {
  local unit_name="$1"
  if [[ "$unit_name" == *.* ]]; then
    printf '%s\n' "$unit_name"
  else
    printf '%s.service\n' "$unit_name"
  fi
}

wait_for_port_8080_to_clear() {
  for _ in $(seq 1 15); do
    if ! ss -ltnp | grep ':8080 ' >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  if ss -ltnp | grep ':8080 ' >/dev/null 2>&1; then
    echo "Port 8080 is still in use after stopping existing quarkus-run.jar processes" >&2
    exit 1
  fi
}

stop_stray_quarkus_processes() {
  local unit_name="$1"
  local unit_main_pid=""

  if command -v systemctl >/dev/null 2>&1; then
    unit_main_pid="$(systemctl show -p MainPID --value "$unit_name" 2>/dev/null || true)"
  fi

  local jar_pids=""
  jar_pids="$(pgrep -f 'quarkus-run.jar' || true)"
  [[ -n "$jar_pids" ]] || return 0

  local stray_pids=""
  local pid
  for pid in $jar_pids; do
    if [[ -n "$unit_main_pid" && "$unit_main_pid" != "0" && "$pid" == "$unit_main_pid" ]]; then
      continue
    fi
    stray_pids="${stray_pids} ${pid}"
  done

  stray_pids="${stray_pids# }"
  if [[ -n "$stray_pids" ]]; then
    echo "Stopping stray quarkus-run.jar process(es): ${stray_pids}" >&2
    kill $stray_pids >/dev/null 2>&1 || true
    sleep 1
    for pid in $stray_pids; do
      if kill -0 "$pid" >/dev/null 2>&1; then
        kill -9 "$pid" >/dev/null 2>&1 || true
      fi
    done
    wait_for_port_8080_to_clear
  fi
}

write_systemd_unit() {
  local unit_output="$1"
  local unit_name
  unit_name="$(normalize_systemd_unit_name "$SERVICE_NAME")"
  cat >"$unit_output" <<EOF
[Unit]
Description=StockTracker SIT backend (${unit_name})
After=network.target

[Service]
Type=simple
User=${DEPLOY_USER}
WorkingDirectory=${DEPLOY_DIR%/}/current
EnvironmentFile=${DEPLOY_DIR%/}/runtime.env
ExecStart=/usr/bin/env bash -lc 'exec "\${JAVA_BIN:-java}" -jar quarkus-run.jar'
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
}

remote_exec() {
  local target="$1"
  shift
  ssh -p "$SSH_PORT" -o BatchMode=yes -o ConnectTimeout=5 "$target" "$@"
}

deploy_remote() {
  local bundle_path="$1"
  local runtime_env_path="$2"
  local systemd_unit_path="$3"
  local target="${DEPLOY_USER}@${APP_HOST}"
  local release_id
  release_id="$(date +%Y%m%d-%H%M%S)"
  local remote_base="${DEPLOY_DIR%/}"
  local remote_release="${remote_base}/releases/${release_id}"
  local remote_bundle="/tmp/stocktracker-sit-${release_id}.tar.gz"
  local remote_env="/tmp/stocktracker-sit-${release_id}.env"
  local remote_unit="/tmp/stocktracker-sit-${release_id}.service"

  log "Copying deployment bundle to ${target}"
  scp -P "$SSH_PORT" -o BatchMode=yes "$bundle_path" "${target}:${remote_bundle}"
  scp -P "$SSH_PORT" -o BatchMode=yes "$runtime_env_path" "${target}:${remote_env}"
  scp -P "$SSH_PORT" -o BatchMode=yes "$systemd_unit_path" "${target}:${remote_unit}"

  log "Activating release ${release_id} on ${APP_HOST}"
  remote_exec "$target" bash -s -- "$remote_base" "$remote_release" "$remote_bundle" "$remote_env" "$remote_unit" "$SERVICE_NAME" "$REMOTE_LOG_FILE" <<'REMOTE'
set -euo pipefail

deploy_base="$1"
release_dir="$2"
bundle_path="$3"
env_path="$4"
unit_upload_path="$5"
service_name="$6"
log_file="$7"

normalize_systemd_unit_name() {
  local unit_name="$1"
  if [[ "$unit_name" == *.* ]]; then
    printf '%s\n' "$unit_name"
  else
    printf '%s.service\n' "$unit_name"
  fi
}

wait_for_port_8080_to_clear() {
  for _ in $(seq 1 15); do
    if ! ss -ltnp | grep ':8080 ' >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  if ss -ltnp | grep ':8080 ' >/dev/null 2>&1; then
    echo "Port 8080 is still in use after stopping existing quarkus-run.jar processes" >&2
    exit 1
  fi
}

stop_stray_quarkus_processes() {
  local unit_name="$1"
  local unit_main_pid=""

  unit_main_pid="$(systemctl show -p MainPID --value "$unit_name" 2>/dev/null || true)"

  local jar_pids=""
  jar_pids="$(pgrep -f 'quarkus-run.jar' || true)"
  [[ -n "$jar_pids" ]] || return 0

  local stray_pids=""
  local pid
  for pid in $jar_pids; do
    if [[ -n "$unit_main_pid" && "$unit_main_pid" != "0" && "$pid" == "$unit_main_pid" ]]; then
      continue
    fi
    stray_pids="${stray_pids} ${pid}"
  done

  stray_pids="${stray_pids# }"
  if [[ -n "$stray_pids" ]]; then
    echo "Stopping stray quarkus-run.jar process(es): ${stray_pids}" >&2
    kill $stray_pids >/dev/null 2>&1 || true
    sleep 1
    for pid in $stray_pids; do
      if kill -0 "$pid" >/dev/null 2>&1; then
        kill -9 "$pid" >/dev/null 2>&1 || true
      fi
    done
    wait_for_port_8080_to_clear
  fi
}

mkdir -p "${deploy_base}/releases" "$release_dir"
tar -xzf "$bundle_path" -C "$release_dir"
mv "$env_path" "${deploy_base}/runtime.env"
ln -sfn "${release_dir}/quarkus-app" "${deploy_base}/current"
rm -f "$bundle_path"

if command -v systemctl >/dev/null 2>&1; then
  unit_name="$(normalize_systemd_unit_name "$service_name")"
  sudo install -m 0644 "$unit_upload_path" "/etc/systemd/system/${unit_name}"
  rm -f "$unit_upload_path"
  sudo systemctl daemon-reload
  sudo systemctl enable "$unit_name" >/dev/null
  echo "Installed systemd unit ${unit_name}" >&2
  if systemctl list-unit-files "$unit_name" >/dev/null 2>&1; then
    stop_stray_quarkus_processes "$unit_name"
    sudo systemctl restart "$unit_name"
    echo "Restarted ${unit_name} via systemctl" >&2
  else
    echo "systemd unit not found for ${unit_name} after installation; falling back to nohup" >&2
  fi
else
  rm -f "$unit_upload_path"
  echo "systemctl not found on remote host; falling back to nohup" >&2
fi

if ! command -v systemctl >/dev/null 2>&1 || ! systemctl list-unit-files "$(normalize_systemd_unit_name "$service_name")" >/dev/null 2>&1; then
  pkill -f 'quarkus-run.jar' >/dev/null 2>&1 || true
  wait_for_port_8080_to_clear
  nohup bash -lc '
    set -a
    source "$1"
    set +a
    cd "$2"
    exec "${JAVA_BIN:-java}" -jar quarkus-run.jar
  ' bash "${deploy_base}/runtime.env" "${deploy_base}/current" >"$log_file" 2>&1 < /dev/null &
fi
REMOTE
}

check_http_ok() {
  local label="$1"
  local url="$2"
  local timeout_seconds="${3:-30}"
  local code=""
  local waited=0

  while (( waited < timeout_seconds )); do
    code=$(curl --silent --show-error --location --max-time 15 --output /dev/null --write-out '%{http_code}' "$url" || true)
    if [[ "$code" =~ ^2[0-9][0-9]$ ]]; then
      log "${label} check OK: ${url} -> ${code}"
      return 0
    fi
    sleep 1
    waited=$((waited + 1))
  done

  fail "${label} check failed for ${url} after ${timeout_seconds}s (last status ${code})"
}

main() {
  local arg
  local override_app_host=""
  local override_db_host=""
  local override_db_port=""
  local override_db_name=""
  local override_db_username=""
  local override_db_password=""
  local override_public_base_url=""
  local override_artifact_path=""
  local override_deploy_user=""
  local override_service_name=""
  local override_deploy_dir=""
  local override_remote_log_file=""
  local override_ssh_port=""
  local override_env_file=""

  while [[ $# -gt 0 ]]; do
    arg="$1"
    case "$arg" in
      --env-file) override_env_file="$2"; shift 2 ;;
      --app-host) override_app_host="$2"; shift 2 ;;
      --db-host) override_db_host="$2"; shift 2 ;;
      --db-port) override_db_port="$2"; shift 2 ;;
      --db-name) override_db_name="$2"; shift 2 ;;
      --db-username) override_db_username="$2"; shift 2 ;;
      --db-password) override_db_password="$2"; shift 2 ;;
      --public-base-url) override_public_base_url="$2"; shift 2 ;;
      --artifact-path) override_artifact_path="$2"; shift 2 ;;
      --deploy-user) override_deploy_user="$2"; shift 2 ;;
      --service-name) override_service_name="$2"; shift 2 ;;
      --deploy-dir) override_deploy_dir="$2"; shift 2 ;;
      --remote-log-file) override_remote_log_file="$2"; shift 2 ;;
      --ssh-port) override_ssh_port="$2"; shift 2 ;;
      --dry-run) DRY_RUN="true"; shift ;;
      --validate-only) VALIDATE_ONLY="true"; shift ;;
      --skip-network-checks) SKIP_NETWORK_CHECKS="true"; shift ;;
      --help) usage; exit 0 ;;
      *) fail "Unknown option: $arg" ;;
    esac
  done

  if [[ -n "$override_env_file" ]]; then
    ENV_FILE="$override_env_file"
  fi

  load_env_file "$ENV_FILE"
  set_override APP_HOST "$override_app_host"
  set_override DB_HOST "$override_db_host"
  set_override DB_PORT "$override_db_port"
  set_override DB_NAME "$override_db_name"
  set_override DB_USERNAME "$override_db_username"
  set_override DB_PASSWORD "$override_db_password"
  set_override PUBLIC_BASE_URL "$override_public_base_url"
  set_override ARTIFACT_PATH "$override_artifact_path"
  set_override DEPLOY_USER "$override_deploy_user"
  set_override SERVICE_NAME "$override_service_name"
  set_override DEPLOY_DIR "$override_deploy_dir"
  set_override REMOTE_LOG_FILE "$override_remote_log_file"
  set_override SSH_PORT "$override_ssh_port"

  require_command curl
  require_command ssh
  require_command scp
  require_command tar
  validate_required
  validate_port "$DB_PORT"
  validate_port "$SSH_PORT"
  probe_tcp "$APP_HOST" "$SSH_PORT" "app host SSH"
  probe_tcp "$DB_HOST" "$DB_PORT" "database"

  local jdbc_url="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
  log "Effective config: profile=${QUARKUS_PROFILE} app_host=${APP_HOST} db_host=${DB_HOST} db_port=${DB_PORT} db_name=${DB_NAME} public_base_url=${PUBLIC_BASE_URL}"
  log "Effective JDBC URL: ${jdbc_url}"

  if [[ "$VALIDATE_ONLY" == "true" ]]; then
    log "Validation complete"
    exit 0
  fi

  if [[ "$DRY_RUN" == "true" ]]; then
    log "Dry run complete; skipped build, upload, and restart"
    exit 0
  fi

  TEMP_BUNDLE_PATH="$(mktemp "${TMPDIR:-/tmp}/stocktracker-sit-bundle.XXXXXX.tar.gz")"
  TEMP_RUNTIME_ENV_PATH="$(mktemp "${TMPDIR:-/tmp}/stocktracker-sit-env.XXXXXX")"
  TEMP_SYSTEMD_UNIT_PATH="$(mktemp "${TMPDIR:-/tmp}/stocktracker-sit-unit.XXXXXX.service")"
  trap cleanup_temp_files EXIT

  write_runtime_env "$TEMP_RUNTIME_ENV_PATH"
  write_systemd_unit "$TEMP_SYSTEMD_UNIT_PATH"
  build_bundle "$TEMP_BUNDLE_PATH"
  deploy_remote "$TEMP_BUNDLE_PATH" "$TEMP_RUNTIME_ENV_PATH" "$TEMP_SYSTEMD_UNIT_PATH"
  check_http_ok "api-health" "${PUBLIC_BASE_URL%/}/q/health"
  log "Deployment complete"
}

main "$@"
