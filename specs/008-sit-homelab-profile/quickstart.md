# Quickstart: SIT Homelab Profile

This feature adds a manual backend deployment path for the private homelab SIT
environment. The frontend remains deployed separately and already points to the
public backend URL.

## Prerequisites

- Access to a machine inside the homelab network
- Shell access from that machine to the backend app host
- `DEPLOY_USER` can run `sudo install`, `sudo systemctl daemon-reload`,
  `sudo systemctl enable`, and `sudo systemctl restart` on the backend app host
- Network reachability from that machine to:
  - the backend app host
  - the MySQL host
  - the public backend URL for final verification
- JDK 21 and Maven wrapper support on the machine running the script

## Default Environment Values

Recommended local file on the homelab machine: `scripts/.env`

```bash
APP_HOST=192.168.100.101
DB_HOST=192.168.100.102
DB_PORT=3306
DB_NAME=stocktracker
DB_USERNAME=stocktracker
DB_PASSWORD=change-me
PUBLIC_BASE_URL=https://example.com
DEPLOY_USER=deploy
SERVICE_NAME=stocktracker-backend-sit
QUARKUS_PROFILE=sit
```

Provide database credentials and any deployment-user-specific values in that
same local `.env` file or through shell overrides on the homelab machine. The
real `scripts/.env` must stay out of git; only a `scripts/.env.example` should
be committed if documentation is needed.

## Planned Deployment Flow

```bash
bash scripts/deploy-homelab-sit.sh --validate-only
bash scripts/deploy-homelab-sit.sh
```

Expected script behavior:

1. Load `scripts/.env` when present, then apply any shell or flag overrides.
2. Validate required inputs and private-network reachability.
3. Build or locate the backend runtime artifact for SIT.
4. Copy the artifact to the app host.
5. Upload or update the systemd unit on the app host, then restart the backend
   process using the `sit` profile.
6. Verify `<public backend URL>/q/health`.

## Manual Verification

After a successful run:

```bash
curl -fsS <public backend URL>/q/health
```

Then open the deployed frontend and confirm it can load data from the SIT
backend without frontend redeployment. A minimal manual non-regression check is:

1. Open the existing frontend URL.
2. Log in with a SIT-capable account if authentication is enabled.
3. Confirm the dashboard or watchlist view loads data successfully from the
   deployed backend without any Vercel/frontend redeploy.

## Non-Goals

- No Vercel/frontend deployment changes
- No GitHub-hosted deployment into the private network
- No AWS production infrastructure changes
