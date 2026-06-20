# Contract: SIT Runtime Configuration

Defines the backend runtime configuration expected for the homelab `sit`
environment.

## Configuration Surface

The backend continues to use `application.properties` with environment-variable
overrides. The `sit` environment adds a dedicated runtime profile and uses
deployment-provided values for host-specific settings. Those deployment values
may be sourced from a local `scripts/.env` file maintained on the homelab
machine.

## Required Runtime Values

| Setting | Purpose | Expected Source |
|--------|---------|-----------------|
| `QUARKUS_PROFILE=sit` | Activate the SIT-specific runtime profile | deployment script |
| `QUARKUS_DATASOURCE_JDBC_URL` | Full JDBC URL for the homelab MySQL instance | deployment script |
| `QUARKUS_DATASOURCE_USERNAME` | Database login user | deployment environment |
| `QUARKUS_DATASOURCE_PASSWORD` | Database login password | deployment environment or secret file outside git |
| `SIT_AUTH_MODE` | Authentication mode override for SIT | deployment environment or `sit` profile default |
| `SIT_DEV_BOOTSTRAP_ENABLED` | Toggle dev bootstrap for SIT | deployment environment or `sit` profile default |
| `SIT_SCHEDULER_ENABLED` | Toggle scheduler for SIT | deployment environment or `sit` profile default |
| `QUARKUS_HTTP_HOST` | Bind address for the app process | `sit` profile default |

## SIT Defaults

| Setting | Default |
|--------|---------|
| app host | `192.168.100.101` |
| db host | `192.168.100.102` |
| db port | `3306` |
| public backend URL | `https://example.com` |

## Validation Rules

- The deployment script must fail if host values are empty.
- The deployment script must fail if the composed JDBC URL is invalid.
- Secrets must be injected at runtime and must not be committed to repository files.
- The SIT profile must not alter production AWS-specific deployment settings.

## Compatibility Rules

- Production AWS deployment remains on its existing path and must not require
  `sit` settings.
- Frontend `VITE_API_BASE_URL` remains unchanged for this feature.
- The same backend codebase must support local dev, AWS production, and SIT
  homelab operation through configuration rather than forks.
- If `scripts/.env` is used, a committed `scripts/.env.example` may document
  expected keys, but the real `.env` file must remain ignored by git.
