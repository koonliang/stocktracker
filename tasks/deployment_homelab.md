# Homelab Deployment Plan for Stock Tracker

## Quick Reference

**Deployment commands (Linux/Mac):**
```bash
# Build frontend + backend together (from project root)
./scripts/build-with-frontend.sh

# Deploy to LXC (from project root)
LXC_HOST=192.168.1.100 ./scripts/deploy.sh

# On LXC - Check service status
systemctl status stocktracker-backend
journalctl -u stocktracker-backend -f

# On LXC - Restart service after updates
sudo systemctl restart stocktracker-backend
```

**Deployment commands (Windows):**
```bat
REM Build frontend + backend together (from project root)
scripts\build-with-frontend.bat

REM Deploy to LXC (from project root)
set LXC_HOST=192.168.1.100
scripts\deploy.bat

REM On LXC - Check service status
systemctl status stocktracker-backend
journalctl -u stocktracker-backend -f

REM On LXC - Restart service after updates
sudo systemctl restart stocktracker-backend
```

**Key files created:**
- `scripts/build-with-frontend.sh` / `.bat` - Build script (combines frontend + backend)
- `scripts/deploy.sh` / `.bat` - Complete deployment automation (builds, deploys to LXC)
- `scripts/stocktracker-backend.service` - Systemd service definition
- `scripts/.env.production.template` - Backend environment variables
- `scripts/init-database.sql` - Database initialization script

## Architecture Overview

```
┌──────────────────────────┐         ┌──────────────────────┐
│  Raspberry Pi            │         │  Proxmox LXC         │
│  (Nginx Proxy Manager)   │ ─────── │  (webapp)            │
│                          │         │                      │
│  - Configure via Web UI  │  HTTP   │  - Spring Boot       │
│  - Proxy to LXC:8080     │ ──────► │  - Port 8080         │
│                          │         │  - Serves React SPA  │
│                          │         │  - Serves /api/*     │
│                          │         │  - Systemd service   │
└──────────────────────────┘         └──────────────────────┘
                                                │
                                                │ JDBC
                                                ▼
                                       ┌─────────────────┐
                                       │  MySQL Server   │
                                       │  (existing)     │
                                       └─────────────────┘
```

**Key difference:** Spring Boot serves BOTH the frontend static files AND the API endpoints. Nginx Proxy Manager simply proxies all requests to the Spring Boot application.

## Configuration Placeholders

The following placeholders will be used in configuration files (replace with actual values during deployment):
- `<LXC_IP>` - IP address or hostname of the LXC container (e.g., 192.168.1.100)
- `<APP_HOSTNAME>` - Hostname for accessing the app on Raspberry Pi (e.g., stocktracker.local)
- `<MYSQL_HOST>` - MySQL server IP/hostname
- `<DB_PASSWORD>` - Database password
- `<JWT_SECRET>` - JWT signing secret (generate secure random string)

## Deployment Components

### 1. Full Stack Deployment (LXC Container)

**Prerequisites on LXC:**
- Java 17 JRE
- systemd (already available in LXC)
- Network connectivity to MySQL server

**What gets deployed:**
Single Spring Boot JAR containing:
- Backend API endpoints (/api/*)
- Frontend React SPA (static files served from root /)
- All dependencies bundled

**Files to create:**
- `scripts/deploy.sh` - Automated deployment script
- `scripts/stocktracker-backend.service` - Systemd service file
- `scripts/.env.production.template` - Environment variables template
- `backend/build-with-frontend.sh` - Build script that combines frontend and backend

**Deployment process:**
1. Build frontend: `cd frontend && npm run build`
2. Copy frontend dist to backend static resources: `cp -r frontend/dist/* backend/src/main/resources/static/`
3. Build backend JAR with frontend included: `cd backend && mvn clean package -DskipTests`
4. SSH to LXC and create directory structure
5. Copy JAR to LXC: `/opt/stocktracker/stocktracker.jar`
6. Copy systemd service file to `/etc/systemd/system/stocktracker-backend.service`
7. Copy environment file to `/opt/stocktracker/.env`
8. Create `stocktracker` user if not exists
9. Set proper file permissions
10. Reload systemd daemon
11. Enable and restart service: `systemctl enable --now stocktracker-backend`

**Service configuration:**
- User: Create dedicated `stocktracker` user for security
- Working directory: `/opt/stocktracker`
- Auto-restart on failure
- Logs via journald: `journalctl -u stocktracker-backend -f`

**Spring Boot static content serving:**
- Spring Boot automatically serves files from `src/main/resources/static/`
- Frontend routes handled by React Router (SPA fallback to index.html)
- API requests to `/api/*` handled by Spring controllers
- Root path `/` serves the React application

### 2. Nginx Proxy Manager Configuration (Manual)

**Setup via NPM Web UI:**
1. Log into Nginx Proxy Manager web interface
2. Add new Proxy Host
3. Configure:
   - Domain Names: `stocktracker.local` (or your chosen hostname)
   - Scheme: `http`
   - Forward Hostname/IP: `<LXC_IP>`
   - Forward Port: `8080`
   - Cache Assets: Enable
   - Block Common Exploits: Enable
   - Websockets Support: Enable (optional, for future real-time features)
4. Optional: Configure SSL certificate (Let's Encrypt or custom)
5. Save configuration

**No config files needed** - all configuration through web UI!

### 3. Environment Configuration

**Backend environment variables (.env):**
```bash
# Database Configuration
DB_HOST=<MYSQL_HOST>
DB_PORT=3306
DB_NAME=stocktracker
DB_USERNAME=stocktracker
DB_PASSWORD=<DB_PASSWORD>

# JWT Configuration
JWT_SECRET=<JWT_SECRET>

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```

**Frontend build configuration:**
No environment variables needed! Since the frontend is served from the same origin as the API (both from Spring Boot), the frontend can use relative paths:
- API calls to `/api/*` automatically go to the same server
- No CORS issues since same-origin

**Optional frontend .env (for build customization):**
```bash
VITE_APP_NAME=Stock Tracker
```

### 4. Database Setup

**MySQL preparation (run on MySQL server):**
```sql
CREATE DATABASE IF NOT EXISTS stocktracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'stocktracker'@'%' IDENTIFIED BY '<password>';
GRANT ALL PRIVILEGES ON stocktracker.* TO 'stocktracker'@'%';
FLUSH PRIVILEGES;
```

**Schema initialization:**
- Option 1: Use Flyway/Liquibase migrations (recommended for production)
- Option 2: Set `spring.jpa.hibernate.ddl-auto=update` for initial setup (then change to `validate`)
- Option 3: Manually run SQL schema from development H2 database

## Implementation Steps

### Step 1: Configure Spring Boot for SPA routing

**File:** `backend/src/main/java/com/stocktracker/config/WebConfig.java` (new file)

Create a configuration class to handle SPA routing - forward all non-API routes to index.html:
- Implement `WebMvcConfigurer`
- Add `ViewControllerRegistry` to forward all paths (except `/api/*`) to `/index.html`
- This ensures React Router handles client-side routing

**Why needed:** When users navigate to `/dashboard` or any React route, Spring Boot needs to serve index.html (not return 404)

### Step 2: Simplify CORS configuration (optional)

**File:** `backend/src/main/java/com/stocktracker/config/CorsConfig.java`

Since frontend and backend are served from the same origin, CORS is not strictly needed. However, you can:
- Keep current CORS config for development (localhost:3000)
- For production, CORS headers are unnecessary (same-origin)
- OR remove CORS config entirely for production profile

### Step 3: Create build and deployment script

**Build script** - `scripts/build-with-frontend.sh`:
```bash
#!/bin/bash
# Build frontend
cd frontend
npm run build

# Copy frontend dist to backend static resources
rm -rf ../backend/src/main/resources/static/*
cp -r dist/* ../backend/src/main/resources/static/

# Build backend with frontend included
cd ../backend
mvn clean package -DskipTests

echo "Build complete: backend/target/stocktracker-backend-*.jar"
```

**Deployment script** - `scripts/deploy.sh`:
- Run build script first
- SSH to LXC and create directory structure: `/opt/stocktracker/`
- Copy JAR file to `/opt/stocktracker/stocktracker.jar`
- Copy `.env` file to `/opt/stocktracker/.env`
- Copy systemd service file to `/etc/systemd/system/stocktracker-backend.service`
- Create `stocktracker` user if not exists (with home dir, no login shell)
- Set proper file permissions (JAR: 755, .env: 600, owner: stocktracker)
- Reload systemd daemon: `systemctl daemon-reload`
- Enable and restart service: `systemctl enable --now stocktracker-backend`
- Show service status and recent logs: `systemctl status stocktracker-backend && journalctl -u stocktracker-backend -n 20`

### Step 4: Create systemd service file

**File:** `scripts/stocktracker-backend.service`

Key configuration:
```ini
[Unit]
Description=Stock Tracker Full Stack Application
After=network.target mysql.service

[Service]
Type=simple
User=stocktracker
WorkingDirectory=/opt/stocktracker
EnvironmentFile=/opt/stocktracker/.env
ExecStart=/usr/bin/java -jar -Xmx512m stocktracker.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

### Step 5: Create environment templates

**File:** `scripts/.env.production.template`
```bash
# Database Configuration
DB_HOST=<MYSQL_HOST>
DB_PORT=3306
DB_NAME=stocktracker
DB_USERNAME=stocktracker
DB_PASSWORD=<DB_PASSWORD>

# JWT Configuration
JWT_SECRET=<JWT_SECRET>

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```

### Step 6: Create database initialization script

**File:** `scripts/init-database.sql`
```sql
CREATE DATABASE IF NOT EXISTS stocktracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'stocktracker'@'%' IDENTIFIED BY '<DB_PASSWORD>';
GRANT ALL PRIVILEGES ON stocktracker.* TO 'stocktracker'@'%';
FLUSH PRIVILEGES;
```

### Step 7: Configure Nginx Proxy Manager

**Manual configuration via NPM web UI:**
1. Access NPM web interface (usually http://raspberry-pi-ip:81)
2. Go to "Proxy Hosts" > "Add Proxy Host"
3. Fill in:
   - **Domain Names:** stocktracker.local (or your hostname)
   - **Scheme:** http
   - **Forward Hostname/IP:** <LXC_IP>
   - **Forward Port:** 8080
   - **Cache Assets:** ON
   - **Block Common Exploits:** ON
   - **Websockets Support:** ON
4. Save and test access

### Step 8: Update README.md

Add comprehensive homelab deployment section with:
- Architecture overview
- Prerequisites (Java 17 on LXC, MySQL, NPM setup)
- Step-by-step deployment instructions
- NPM configuration guide
- Environment variable setup
- Database initialization
- Troubleshooting common issues
- Service management commands

## Security Considerations

1. **Backend service user:** Run as dedicated non-root user (`stocktracker`)
2. **File permissions:**
   - JAR file: 755 (executable by stocktracker user)
   - .env file: 600 (owner read/write only)
   - Working directory: owned by stocktracker user
3. **JWT secret:** Generate strong random secret (64+ characters, use `openssl rand -base64 64`)
4. **Database credentials:**
   - Use strong password for stocktracker DB user
   - Limit privileges to only the stocktracker database
   - Use host-specific user (`'stocktracker'@'<LXC_IP>'`) instead of wildcard if possible
5. **Nginx Proxy Manager:**
   - Consider enabling SSL/TLS certificate via Let's Encrypt
   - Enable "Block Common Exploits" option
   - Consider enabling authentication if exposing externally
6. **Network:**
   - Ensure Spring Boot port 8080 is only accessible from NPM, not externally
   - Use firewall rules if needed
   - Consider using private network for LXC-MySQL communication

## Post-Deployment

**Verification steps:**
1. Check service status: `systemctl status stocktracker-backend`
2. View logs: `journalctl -u stocktracker-backend -n 50`
3. Test direct access to backend (from LXC or network): `curl http://<LXC_IP>:8080/api/health`
4. Access application via NPM hostname: `http://stocktracker.local`
5. Verify frontend loads correctly
6. Test login/registration
7. Test creating a holding/transaction
8. Verify data persists in MySQL database

**Monitoring:**
- Application logs: `journalctl -u stocktracker-backend -f`
- Filter for errors: `journalctl -u stocktracker-backend -p err -f`
- Check service health: `systemctl status stocktracker-backend`
- NPM access logs: Available in NPM web UI under "Audit Log"

## Critical Files to Create/Modify

**New files to create:**
- `scripts/build-with-frontend.sh` - Build script that combines frontend and backend
- `scripts/deploy.sh` - Complete deployment automation script
- `scripts/stocktracker-backend.service` - Systemd service definition
- `scripts/.env.production.template` - Environment variables template
- `scripts/init-database.sql` - Database initialization script
- `backend/src/main/java/com/stocktracker/config/WebConfig.java` - SPA routing configuration

**Files to update:**
- `README.md` - Add homelab deployment section
- `backend/src/main/java/com/stocktracker/config/CorsConfig.java` (optional) - Simplify or remove CORS config for production
- `backend/pom.xml` - Ensure static resources are included in JAR build

**Frontend build integration:**
- Frontend `dist/` files will be copied to `backend/src/main/resources/static/`
- Spring Boot automatically serves these files
- React Router handles client-side routing

## Summary

This plan creates a **simplified, production-ready deployment** for your homelab using Nginx Proxy Manager:

**Architecture highlights:**
- Single Spring Boot JAR containing both frontend and backend
- Nginx Proxy Manager proxies all traffic to Spring Boot
- No separate web server needed
- No CORS issues (same-origin)
- One service to manage

**What gets created:**
1. Automated build script to bundle frontend with backend
2. Automated deployment script with SSH to LXC
3. Systemd service for reliable operation with auto-restart
4. Spring Boot SPA routing configuration
5. Environment configuration templates with placeholders
6. Database initialization script
7. Comprehensive deployment documentation

**Deployment flow:**
1. Build frontend → Copy to backend static resources
2. Build backend JAR (includes frontend)
3. Deploy JAR to LXC via SSH
4. Install as systemd service
5. Configure Nginx Proxy Manager via web UI

**Key benefits:**
- **Simplified architecture:** One JAR, one service, one deployment
- **No CORS complexity:** Same-origin for frontend and backend
- **Automated deployment:** Single script handles everything
- **Easy monitoring:** Single systemd service, one log stream
- **Auto-recovery:** Service auto-starts on boot and recovers from failures
- **User-friendly proxy:** NPM web UI instead of manual nginx configs

**Before running:**
- Replace placeholders in .env template with actual values
- Ensure SSH access is configured to LXC
- Verify MySQL is accessible from LXC container
- Ensure Java 17 is installed on LXC
- Have NPM web UI credentials ready for proxy configuration
- Run database initialization script on MySQL server

**Advantages over traditional separate deployment:**
- Fewer moving parts (no separate nginx config files)
- Easier updates (single JAR to deploy)
- No CORS configuration needed
- Simpler troubleshooting (one service to check)
- Better suited for homelab simplicity
