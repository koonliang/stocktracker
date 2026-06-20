# Data Model: SIT Homelab Profile

## SIT Deployment Profile

A named backend deployment target representing the homelab system-integration
environment.

### Fields

- **name**: fixed environment identifier, `sit`
- **public_backend_url**: externally reachable backend base URL used by the
  existing frontend
- **quarkus_profile**: runtime profile value used by the backend process
- **deployment_mode**: manual operator-run deployment from inside the homelab
- **frontend_scope**: reference that frontend deployment is out of scope and
  unchanged

### Validation Rules

- `name` must remain unique among deployment environments.
- `public_backend_url` must be an HTTPS URL.
- `deployment_mode` for this feature is always manual script execution.

## Deployment Host Configuration

Operator-managed connection values that identify homelab targets.

### Fields

- **app_host**: backend application server hostname or IP
- **db_host**: MySQL server hostname or IP
- **db_port**: MySQL port, default `3306`
- **db_name**: target schema name for SIT
- **db_username**: runtime database username
- **db_password_source**: where the deployment script obtains the password
  without committing it to the repository
- **env_file_path**: optional local file path for deployment inputs, expected
  to be `scripts/.env` when used
- **deploy_user**: remote user used to copy artifacts and restart the app
- **service_name**: backend service identifier on the app host

### Validation Rules

- `app_host` and `db_host` are required before deployment starts.
- Host values must be network-reachable from the machine executing the script.
- Secret values must not be echoed to stdout or stored in committed files.
- If an env file is used, it must remain local and excluded from git.

## SIT Backend Deployment Run

A single execution of the homelab deployment script.

### Fields

- **started_at**: timestamp when the script begins
- **target_profile**: expected to be `sit`
- **artifact_path**: built backend package location
- **config_snapshot**: effective host and runtime configuration used for the run
- **preflight_status**: result of config and connectivity validation
- **rollout_status**: result of copy/restart steps on the app host
- **verification_status**: result of public health check
- **failure_reason**: first blocking error, if any

### State Transitions

```text
prepared -> validating -> deploying -> verifying -> succeeded
prepared -> validating -> failed
deploying -> failed
verifying -> failed
```

### Notes

- A run is considered successful only after the public health endpoint passes.
- Failed validation must stop the run before any rollout work is attempted.
