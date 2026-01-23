# Homelab Deployment Plan: Deployment_homelab_02.md

## Overview

Update homelab deployment documentation from Java/Spring Boot (Deployment_homelab_01.md) to **Next.js monorepo** deployment using PM2, PostgreSQL, and Nginx Proxy Manager.

**Key Changes:**
- Application: Java Spring Boot → Next.js 15 full-stack monorepo
- Runtime: Java 17 JRE → Node.js 18+ with PM2 process manager
- Build: Maven JAR → Next.js build (.next directory)
- Database: MySQL → PostgreSQL (self-hosted)
- Migrations: Automatic Prisma migrations during deployment

## Architecture

```
Raspberry Pi (NPM) → LXC Container (PM2 + Next.js) → PostgreSQL Server
Port 80/443         → Port 3000                    → Port 5432
```

## Critical Files to Create

### 1. `/ecosystem.config.cjs` (NEW)
PM2 configuration with:
- Cluster mode (2 instances)
- Auto-restart on failure
- Memory limit: 1GB
- Log management
- Health monitoring

### 2. `/scripts/deploy-nextjs.sh` (NEW)
Automated deployment script that:
1. Builds Next.js application locally
2. Copies build artifacts to LXC via SSH
3. Installs production dependencies
4. Runs Prisma migrations automatically
5. Starts/restarts PM2 process
6. Shows deployment status

### 3. `/scripts/deploy-nextjs.bat` (NEW)
Windows version of deployment script with same functionality.

### 4. `/scripts/init-postgres.sql` (NEW)
PostgreSQL initialization script:
- Creates `stocktracker` database
- Creates `stocktracker` user with password
- Grants all privileges
- Sets up schema permissions for Prisma

### 5. `/.env.production` (NEW - from template)
Production environment variables:
- `DATABASE_URL` - PostgreSQL connection string
- `DIRECT_URL` - Direct PostgreSQL connection (for migrations)
- `JWT_SECRET` - Generated with `openssl rand -base64 32`
- `CRON_SECRET` - For cron job authentication
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` - OAuth2
- `OAUTH2_REDIRECT_URI` - Production callback URL
- `YAHOO_FINANCE_CHART_URL` - External API
- `NODE_ENV=production`
- `PORT=3000`

### 6. `/src/app/api/health/route.ts` (NEW)
Health check endpoint that:
- Tests Prisma database connection
- Returns status: healthy/unhealthy
- Includes uptime and timestamp
- Used by monitoring tools

### 7. `/tasks/Deployment/Deployment_homelab_02.md` (NEW)
Updated deployment documentation covering:
- Architecture overview
- Prerequisites (Node.js 18+, PM2, PostgreSQL)
- Step-by-step deployment instructions
- PostgreSQL setup guide
- Environment variable configuration
- PM2 commands and monitoring
- Nginx Proxy Manager configuration
- Troubleshooting guide
- Cron job setup for demo account cleanup
- Security best practices

## Implementation Steps

### Phase 1: Create Configuration Files (30 min)
1. Create `ecosystem.config.cjs` with PM2 cluster configuration
2. Create `.env.production.template` with all required variables
3. Create `src/app/api/health/route.ts` health check endpoint

### Phase 2: Create Deployment Scripts (45 min)
1. Create `scripts/deploy-nextjs.sh` with full automation:
   - Build Next.js locally
   - Copy artifacts via SCP
   - Install production dependencies
   - Run Prisma migrations
   - Setup PM2 with systemd
   - Start/reload application
2. Create `scripts/deploy-nextjs.bat` (Windows version)
3. Create `scripts/init-postgres.sql` database setup script
4. Create `scripts/setup-cron.sh` for demo account cleanup

### Phase 3: Create Documentation (45 min)
1. Create `tasks/Deployment/Deployment_homelab_02.md` with:
   - Architecture diagram
   - Prerequisites checklist
   - Database setup instructions
   - Environment variable guide
   - Deployment workflow
   - NPM configuration (manual via web UI)
   - Monitoring and troubleshooting
   - Security considerations
   - Post-deployment verification

### Phase 4: Cron Job Configuration (15 min)
Replace Vercel Cron with system cron:
1. Create cron job script that calls `/api/cron/cleanup-demo-accounts`
2. Add to system crontab: `0 2 * * * curl -H "Authorization: Bearer $CRON_SECRET" http://localhost:3000/api/cron/cleanup-demo-accounts`
3. Document in Deployment_homelab_02.md

## Key Differences from Deployment_homelab_01.md

| Aspect | Old (Java) | New (Node.js) |
|--------|-----------|---------------|
| Runtime | Java 17 JRE | Node.js 18+ |
| Process Manager | systemd service | PM2 with systemd integration |
| Build Output | Single JAR file | .next directory + node_modules |
| Deployment | Copy JAR | Copy build artifacts |
| Database | MySQL | PostgreSQL |
| Migrations | Flyway/manual | Prisma (automatic) |
| Port | 8080 | 3000 |
| Clustering | N/A | PM2 cluster mode (2 instances) |
| Hot Reload | Restart service | PM2 reload (zero-downtime) |

## Deployment Workflow

```bash
# One-time PostgreSQL setup
psql -U postgres -h <POSTGRES_HOST> < scripts/init-postgres.sql

# Create production environment file
cp .env.local.template .env.production
# Edit .env.production with actual values

# Deploy to LXC
LXC_HOST=192.168.1.100 ./scripts/deploy-nextjs.sh

# Configure Nginx Proxy Manager (manual via web UI)
# Domain: stocktracker.local
# Forward to: <LXC_IP>:3000
# Enable: Cache, Block Exploits, Websockets
```

## PM2 Management Commands

```bash
# On LXC (as stocktracker user)
pm2 status                    # Show all processes
pm2 logs stocktracker         # View logs
pm2 monit                     # Real-time dashboard
pm2 restart stocktracker      # Restart app
pm2 reload stocktracker       # Zero-downtime reload
pm2 describe stocktracker     # Detailed info
```

## Environment Variables Template

```env
DATABASE_URL=postgresql://stocktracker:<PASSWORD>@<POSTGRES_HOST>:5432/stocktracker
DIRECT_URL=postgresql://stocktracker:<PASSWORD>@<POSTGRES_HOST>:5432/stocktracker
JWT_SECRET=<openssl rand -base64 32>
JWT_EXPIRATION=86400000
GOOGLE_CLIENT_ID=<from Google Cloud Console>
GOOGLE_CLIENT_SECRET=<from Google Cloud Console>
OAUTH2_REDIRECT_URI=https://yourdomain.com/oauth2/redirect
YAHOO_FINANCE_CHART_URL=https://query1.finance.yahoo.com/v8/finance/chart
NEXT_PUBLIC_API_URL=/api
CRON_SECRET=<openssl rand -base64 32>
NODE_ENV=production
PORT=3000
```

## PostgreSQL Setup Script

```sql
CREATE DATABASE stocktracker WITH ENCODING = 'UTF8';
CREATE USER stocktracker WITH ENCRYPTED PASSWORD '<PASSWORD>';
GRANT ALL PRIVILEGES ON DATABASE stocktracker TO stocktracker;
\c stocktracker
GRANT ALL ON SCHEMA public TO stocktracker;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO stocktracker;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO stocktracker;
```

## PM2 Ecosystem Configuration

```javascript
module.exports = {
  apps: [{
    name: 'stocktracker',
    script: 'node_modules/next/dist/bin/next',
    args: 'start',
    instances: 2,
    exec_mode: 'cluster',
    env: { NODE_ENV: 'production', PORT: 3000 },
    max_memory_restart: '1G',
    autorestart: true,
    watch: false
  }]
};
```

## Security Considerations

1. **Application User**: Run as dedicated `stocktracker` user (non-root)
2. **File Permissions**:
   - `.env`: 600 (read/write owner only)
   - Application directory: owned by stocktracker
3. **Secrets**: Generate with `openssl rand -base64 32`
4. **Database**: Restrict PostgreSQL access to LXC IP only
5. **Network**: Firewall rules to restrict port 3000 to NPM only
6. **PM2 Logs**: Rotate logs to prevent disk filling

## Post-Deployment Verification

1. Check health endpoint: `curl http://<LXC_IP>:3000/api/health`
2. Test via NPM: `curl http://stocktracker.local/api/health`
3. Check PM2 status: `pm2 status`
4. View logs: `pm2 logs stocktracker --lines 50`
5. Test authentication: POST to `/api/auth/login`
6. Verify database: `psql -U stocktracker -h <HOST> -d stocktracker -c "\dt"`

## Troubleshooting

- **PM2 not starting**: Check logs with `pm2 logs stocktracker --err`
- **Database connection failed**: Verify `DATABASE_URL` in `.env` and PostgreSQL access
- **Build fails**: Run `npm ci && npm run build` locally to debug
- **Migration fails**: Check Prisma schema matches PostgreSQL
- **Port 3000 in use**: Check with `lsof -i :3000` or `netstat -tulpn | grep 3000`

## Success Criteria

- ✅ Health endpoint returns 200 status
- ✅ PM2 shows 2 instances running
- ✅ Application accessible via NPM hostname
- ✅ Login/authentication works
- ✅ Portfolio data loads correctly
- ✅ Prisma migrations applied successfully
- ✅ PM2 survives system reboot (systemd startup)
- ✅ Logs rotating properly
- ✅ Cron job for demo cleanup configured
