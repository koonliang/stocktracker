# Stock Tracker

A full-stack web application for tracking stock portfolios and watchlists with real-time pricing data from Yahoo Finance.

## Features

- User authentication with JWT tokens
- Real-time stock portfolio tracking
- Live price updates from Yahoo Finance API
- Automatic calculation of returns and performance metrics
- Dashboard with portfolio overview
- Watchlist management
- Responsive design with TailwindCSS

## Tech Stack

### Frontend
- **Framework**: React 19.2.0
- **Build Tool**: Vite 7.2.4
- **Language**: TypeScript 5.9.3
- **Routing**: React Router DOM 7.11.0
- **HTTP Client**: Axios 1.13.2
- **Styling**: TailwindCSS 3.4.19
- **Code Quality**: ESLint, Prettier

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: H2 (development), MySQL (production)
- **Security**: Spring Security with JWT
- **ORM**: Spring Data JPA
- **Caching**: Caffeine
- **API Documentation**: SpringDoc OpenAPI
- **Build Tool**: Maven

### External APIs
- **Yahoo Finance** - Real-time stock quotes and pricing data

## Project Structure

```
stocktracker/
├── backend/                 # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/stocktracker/
│   │   │   │   ├── client/          # Yahoo Finance API client
│   │   │   │   ├── config/          # Configuration (Security, Cache)
│   │   │   │   ├── controller/      # REST controllers
│   │   │   │   ├── dto/             # Data Transfer Objects
│   │   │   │   ├── entity/          # JPA entities
│   │   │   │   ├── repository/      # Database repositories
│   │   │   │   └── service/         # Business logic
│   │   │   └── resources/
│   │   │       ├── application.yml  # Application configuration
│   │   │       └── data.sql         # Seed data
│   │   └── test/
│   └── pom.xml
├── frontend/                # React frontend
│   ├── src/
│   │   ├── components/      # Reusable UI components
│   │   ├── hooks/           # Custom React hooks
│   │   ├── pages/           # Page components
│   │   ├── services/        # API services
│   │   ├── types/           # TypeScript types
│   │   └── utils/           # Utility functions
│   └── package.json
└── tasks/                   # Project task documentation

```

## Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- Maven 3.6+
- npm or yarn

### Backend Setup

1. Navigate to the backend directory:
```bash
cd backend
```

2. Install dependencies and build:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

The backend server will start on `http://localhost:8080`

### Frontend Setup

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm run dev
```

The frontend application will start on `http://localhost:5173`

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration

### Portfolio
- `GET /api/portfolio` - Get user's portfolio with live prices
- `GET /api/portfolio/refresh` - Force refresh prices (bypasses cache)

### Holdings
- `GET /api/holdings` - Get all user holdings
- `POST /api/holdings` - Add a new holding
- `PUT /api/holdings/{id}` - Update a holding
- `DELETE /api/holdings/{id}` - Delete a holding

## Key Features

### Portfolio Dashboard
- View all holdings with real-time prices
- Calculate total returns (dollar and percentage)
- Display current value vs. cost basis
- Portfolio summary with total value, cost, and returns
- Automatic price caching (2-minute TTL)
- Manual refresh option

### Stock Data Integration
- Integration with Yahoo Finance API for real-time quotes
- Batch price fetching for multiple symbols
- Automatic retry and error handling
- Cached responses to reduce API calls

### Security
- JWT-based authentication
- BCrypt password hashing
- Secured API endpoints
- CORS configuration

## Development Scripts

### Frontend
```bash
npm run dev          # Start development server
npm run build        # Build for production
npm run preview      # Preview production build
npm run lint         # Run ESLint
npm run lint:fix     # Fix ESLint issues
npm run format       # Format code with Prettier
```

### Backend
```bash
mvn spring-boot:run  # Run the application
mvn test             # Run tests
mvn clean install    # Clean and build
```

## Homelab Deployment

This section describes how to deploy Stock Tracker to your homelab environment using a combined frontend+backend JAR served from a single Spring Boot application.

### Architecture Overview

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

**Key advantage:** Single JAR deployment - Spring Boot serves both the React frontend (static files) AND the API endpoints. No separate nginx config needed, no CORS issues!

### Prerequisites

#### On LXC Container:
- Java 17 JRE installed
- SSH access configured
- systemd available (standard in LXC)
- Network connectivity to MySQL server

#### On MySQL Server:
- MySQL 5.7+ or 8.0+
- Network accessible from LXC container

#### On Raspberry Pi (optional):
- Nginx Proxy Manager installed and configured
- Network access to LXC container

#### On Development Machine:
- SSH access to LXC container
- Node.js 18+ and npm
- Java 17+ and Maven

### Quick Start

**Linux/Mac:**
```bash
# 1. Build frontend + backend together (from project root)
./scripts/build-with-frontend.sh

# 2. Configure environment variables
cp scripts/.env.production.template scripts/.env.production
# Edit .env.production with your values

# 3. Deploy to LXC
LXC_HOST=192.168.1.100 ./scripts/deploy.sh

# 4. Configure Nginx Proxy Manager via web UI (see below)
```

**Windows:**
```bat
REM 1. Build frontend + backend together (from project root)
scripts\build-with-frontend.bat

REM 2. Configure environment variables
copy scripts\.env.production.template scripts\.env.production
REM Edit .env.production with your values

REM 3. Deploy to LXC
set LXC_HOST=192.168.1.100
scripts\deploy.bat

REM 4. Configure Nginx Proxy Manager via web UI (see below)
```

**Note for Windows users:** You'll need SSH/SCP tools installed (Git for Windows, Windows OpenSSH, or WSL).

### Detailed Deployment Steps

#### Step 1: Database Setup

First, initialize the MySQL database on your MySQL server:

```bash
# On MySQL server, edit the init script with your password
sed 's/<DB_PASSWORD>/your-secure-password/g' scripts/init-database.sql > /tmp/init-db.sql

# Run the initialization
mysql -u root -p < /tmp/init-db.sql

# Clean up
rm /tmp/init-db.sql
```

Or manually create the database:
```sql
CREATE DATABASE stocktracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'stocktracker'@'%' IDENTIFIED BY 'your-secure-password';
GRANT ALL PRIVILEGES ON stocktracker.* TO 'stocktracker'@'%';
FLUSH PRIVILEGES;
```

#### Step 2: Configure Environment Variables

```bash
# Copy the template
cp scripts/.env.production.template scripts/.env.production

# Generate a secure JWT secret
openssl rand -base64 64

# Edit the file with your actual values
nano scripts/.env.production
```

Replace these placeholders:
- `<MYSQL_HOST>`: Your MySQL server IP or hostname (e.g., `192.168.1.50`)
- `<DB_PASSWORD>`: The password you set in Step 1
- `<JWT_SECRET>`: The random string generated above

Example `.env.production`:
```bash
DB_HOST=192.168.1.50
DB_PORT=3306
DB_NAME=stocktracker
DB_USERNAME=stocktracker
DB_PASSWORD=MySecurePassword123!
JWT_SECRET=YourGeneratedJWTSecretHere...
SPRING_PROFILES_ACTIVE=prod
```

**Important:** Protect this file!
```bash
chmod 600 scripts/.env.production
```

#### Step 3: Build the Application

The build script combines frontend and backend into a single JAR:

```bash
./scripts/build-with-frontend.sh
```

This script:
1. Builds the React frontend (`npm run build`)
2. Copies frontend dist files to `backend/src/main/resources/static/`
3. Builds the Spring Boot JAR with frontend included (`mvn clean package`)
4. Outputs: `backend/target/stocktracker-backend-*.jar`

#### Step 4: Deploy to LXC Container

Deploy the JAR to your LXC container:

```bash
# Set the LXC host IP and deploy
LXC_HOST=192.168.1.100 ./scripts/deploy.sh

# Or with custom SSH user/port
LXC_HOST=192.168.1.100 LXC_USER=admin LXC_PORT=2222 ./scripts/deploy.sh
```

The deployment script:
1. Runs the build script
2. Connects to LXC via SSH
3. Creates `/opt/stocktracker/` directory
4. Copies JAR file to `/opt/stocktracker/stocktracker.jar`
5. Copies environment file to `/opt/stocktracker/.env`
6. Installs systemd service
7. Creates `stocktracker` user (if needed)
8. Sets proper file permissions
9. Starts the service
10. Shows service status and logs

#### Step 5: Verify Deployment

```bash
# Check service status
ssh root@192.168.1.100 'systemctl status stocktracker-backend'

# View logs
ssh root@192.168.1.100 'journalctl -u stocktracker-backend -f'

# Test direct access (from LXC or network)
curl http://192.168.1.100:8080/api/health
```

#### Step 6: Configure Nginx Proxy Manager

Configure NPM via its web interface (usually `http://raspberry-pi-ip:81`):

1. Go to **Proxy Hosts** → **Add Proxy Host**
2. Configure the **Details** tab:
   - **Domain Names:** `stocktracker.local` (or your preferred hostname)
   - **Scheme:** `http`
   - **Forward Hostname/IP:** `192.168.1.100` (your LXC IP)
   - **Forward Port:** `8080`
   - **Cache Assets:** Enable
   - **Block Common Exploits:** Enable
   - **Websockets Support:** Enable (optional, for future features)
3. Optional: Configure SSL certificate in the **SSL** tab
4. Save and test access

#### Step 7: Access Your Application

```bash
# Via Nginx Proxy Manager
http://stocktracker.local

# Or directly via LXC IP (not recommended for regular use)
http://192.168.1.100:8080
```

### Service Management

```bash
# View real-time logs
ssh root@192.168.1.100 'journalctl -u stocktracker-backend -f'

# Check service status
ssh root@192.168.1.100 'systemctl status stocktracker-backend'

# Restart service
ssh root@192.168.1.100 'systemctl restart stocktracker-backend'

# Stop service
ssh root@192.168.1.100 'systemctl stop stocktracker-backend'

# View last 50 log entries
ssh root@192.168.1.100 'journalctl -u stocktracker-backend -n 50'

# Filter for errors only
ssh root@192.168.1.100 'journalctl -u stocktracker-backend -p err'
```

### Updating the Application

To deploy updates:

**Linux/Mac:**
```bash
# 1. Build the latest version
./scripts/build-with-frontend.sh

# 2. Deploy to LXC (will automatically restart service)
LXC_HOST=192.168.1.100 ./scripts/deploy.sh
```

**Windows:**
```bat
REM 1. Build the latest version
scripts\build-with-frontend.bat

REM 2. Deploy to LXC (will automatically restart service)
set LXC_HOST=192.168.1.100
scripts\deploy.bat
```

### Troubleshooting

#### Application won't start

```bash
# Check logs for errors
ssh root@192.168.1.100 'journalctl -u stocktracker-backend -n 100'

# Common issues:
# - Database connection failed: Check DB_HOST, DB_PASSWORD in .env
# - Port already in use: Check if another service is using port 8080
# - Permission denied: Verify stocktracker user has access to files
```

#### Can't connect to database

```bash
# Test MySQL connectivity from LXC
ssh root@192.168.1.100 'mysql -h <MYSQL_HOST> -u stocktracker -p stocktracker'

# Check firewall rules on MySQL server
# Verify user grants: SELECT User, Host FROM mysql.user WHERE User = 'stocktracker';
```

#### Frontend routes return 404

This shouldn't happen with the WebConfig, but if it does:
- Verify `WebConfig.java` is included in the build
- Check that frontend files are in the JAR: `jar tf stocktracker.jar | grep static/index.html`
- Verify Spring Boot is serving static content correctly

#### Service keeps restarting

```bash
# Check for crash loop
ssh root@192.168.1.100 'systemctl status stocktracker-backend'

# View detailed logs
ssh root@192.168.1.100 'journalctl -u stocktracker-backend -n 200'

# Common causes:
# - Out of memory: Increase -Xmx in systemd service file
# - Configuration error: Check .env file syntax
# - Missing dependencies: Verify Java 17 is installed
```

### Security Considerations

1. **Service User:** Application runs as dedicated `stocktracker` user (non-root)
2. **File Permissions:**
   - JAR file: `755` (executable)
   - `.env` file: `600` (owner read/write only)
3. **JWT Secret:** Use a strong random secret, regenerate periodically
4. **Database:**
   - Use strong password
   - Consider host-specific user instead of wildcard (`'stocktracker'@'192.168.1.100'`)
   - Enable MySQL SSL/TLS if possible
5. **Network:**
   - Port 8080 should only be accessible from NPM and your local network
   - Configure firewall rules if needed
   - Use SSL/TLS certificate via NPM for HTTPS
6. **Backups:** Regularly backup:
   - MySQL database
   - `.env.production` file (store securely!)

### Performance Tuning

#### Memory Settings

Edit `scripts/stocktracker-backend.service`:
```ini
# Increase for larger portfolios or more concurrent users
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar stocktracker.jar
```

#### Database Connection Pool

Add to `scripts/.env.production`:
```bash
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5
```

### Files Created

- `backend/src/main/java/com/stocktracker/config/WebConfig.java` - SPA routing config
- `scripts/build-with-frontend.sh` - Build script for Linux/Mac (combines frontend + backend)
- `scripts/build-with-frontend.bat` - Build script for Windows (combines frontend + backend)
- `scripts/deploy.sh` - Automated deployment script for Linux/Mac
- `scripts/deploy.bat` - Automated deployment script for Windows
- `scripts/stocktracker-backend.service` - Systemd service definition
- `scripts/.env.production.template` - Environment variables template
- `scripts/.env.production` - Your actual environment config (not in git)
- `scripts/init-database.sql` - Database initialization script

## Database Schema

### Users Table
- `id` (Primary Key)
- `email` (Unique)
- `password` (Hashed)
- `created_at`
- `updated_at`

### Holdings Table
- `id` (Primary Key)
- `user_id` (Foreign Key)
- `symbol` (Stock ticker)
- `company_name`
- `shares` (Supports fractional shares)
- `average_cost` (Cost per share)
- `created_at`
- `updated_at`

### Transactions Table
- `id` (Primary Key)
- `user_id` (Foreign Key)
- `type` (BUY or SELL)
- `symbol` (Stock ticker, max 10 characters)
- `company_name` (max 100 characters)
- `transaction_date` (Date of transaction, cannot be future-dated)
- `shares` (Number of shares, supports fractional shares with precision 12,4)
- `price_per_share` (Price paid/received per share, precision 10,2)
- `total_amount` (Calculated: shares × price_per_share, precision 14,2)
- `notes` (Optional transaction notes, max 500 characters)
- `created_at`
- `updated_at`
- Indexes:
  - `idx_user_symbol` on (user_id, symbol)
  - `idx_user_date` on (user_id, transaction_date)

## Environment Configuration

### Backend
Configure in `backend/src/main/resources/application.yml`:
- Database connection
- JWT secret key
- Yahoo Finance API URL
- Cache settings

### Frontend
Configure in `frontend/vite.config.ts`:
- API base URL
- Proxy settings

## Caching Strategy

The application uses Caffeine cache to optimize Yahoo Finance API calls:
- Portfolio data cached for 2 minutes
- Automatic expiration and refresh
- Manual refresh endpoint available
- Maximum cache size: 1000 entries

## Design System

The UI follows a "Corporate Trust" design system with:
- Professional color palette (Slate, Indigo, Emerald)
- Consistent spacing and typography
- Accessible color contrast
- Responsive breakpoints
- TailwindCSS utility classes

## Future Enhancements

- Sortable table columns
- Search and filter holdings
- Watchlist functionality expansion
- Real-time updates via WebSocket
- Price charts and historical data
- Export to CSV
- Multiple portfolio support
- Market hours indicator
- Performance analytics