#!/bin/bash
set -e  # Exit on any error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration - can be overridden by environment variables
LXC_HOST="${LXC_HOST:-}"
LXC_USER="${LXC_USER:-root}"
LXC_PORT="${LXC_PORT:-22}"

# Get the project root directory (parent of scripts)
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPTS_DIR="$PROJECT_ROOT/scripts"

# Check if LXC_HOST is set
if [ -z "$LXC_HOST" ]; then
    echo -e "${RED}Error: LXC_HOST environment variable is not set${NC}"
    echo -e "Usage: LXC_HOST=<ip-or-hostname> ./scripts/deploy.sh"
    echo -e "Example: LXC_HOST=192.168.1.100 ./scripts/deploy.sh"
    echo -e "\nOptional variables:"
    echo -e "  LXC_USER=<username>  (default: root)"
    echo -e "  LXC_PORT=<port>      (default: 22)"
    exit 1
fi

echo -e "${GREEN}=== Stock Tracker Deployment Script ===${NC}"
echo -e "${BLUE}Target: $LXC_USER@$LXC_HOST:$LXC_PORT${NC}"
echo

# Step 1: Build the application
echo -e "${YELLOW}[1/9] Building application...${NC}"
"$SCRIPTS_DIR/build-with-frontend.sh"
if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Build complete${NC}"

# Find the JAR file
JAR_FILE=$(ls -1 "$PROJECT_ROOT/backend/target"/stocktracker*.jar 2>/dev/null | grep -v "original" | head -1)
if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}Error: JAR file not found!${NC}"
    exit 1
fi

# Step 2: Check if .env file exists
echo -e "\n${YELLOW}[2/9] Checking environment configuration...${NC}"
if [ ! -f "$SCRIPTS_DIR/.env.production" ]; then
    echo -e "${RED}Error: .env.production not found!${NC}"
    echo -e "Please create $SCRIPTS_DIR/.env.production from the template:"
    echo -e "  cp $SCRIPTS_DIR/.env.production.template $SCRIPTS_DIR/.env.production"
    echo -e "  Then edit .env.production with your actual values"
    exit 1
fi
echo -e "${GREEN}✓ Environment file found${NC}"

# Step 3: Test SSH connection
echo -e "\n${YELLOW}[3/9] Testing SSH connection...${NC}"
ssh -p "$LXC_PORT" "$LXC_USER@$LXC_HOST" "echo 'SSH connection successful'" > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Cannot connect to $LXC_USER@$LXC_HOST:$LXC_PORT${NC}"
    echo -e "Please check:"
    echo -e "  1. SSH is running on the LXC container"
    echo -e "  2. SSH keys are set up (or you'll be prompted for password)"
    echo -e "  3. Host and port are correct"
    exit 1
fi
echo -e "${GREEN}✓ SSH connection successful${NC}"

# Step 4: Create directory structure on LXC
echo -e "\n${YELLOW}[4/9] Creating directory structure on LXC...${NC}"
ssh -p "$LXC_PORT" "$LXC_USER@$LXC_HOST" "mkdir -p /opt/stocktracker"
echo -e "${GREEN}✓ Directory created${NC}"

# Step 5: Copy JAR file
echo -e "\n${YELLOW}[5/9] Copying JAR file to LXC...${NC}"
scp -P "$LXC_PORT" "$JAR_FILE" "$LXC_USER@$LXC_HOST:/opt/stocktracker/stocktracker.jar"
echo -e "${GREEN}✓ JAR file copied${NC}"

# Step 6: Copy environment file
echo -e "\n${YELLOW}[6/9] Copying environment file...${NC}"
scp -P "$LXC_PORT" "$SCRIPTS_DIR/.env.production" "$LXC_USER@$LXC_HOST:/opt/stocktracker/.env"
echo -e "${GREEN}✓ Environment file copied${NC}"

# Step 7: Copy and install systemd service
echo -e "\n${YELLOW}[7/9] Installing systemd service...${NC}"
scp -P "$LXC_PORT" "$SCRIPTS_DIR/stocktracker-backend.service" "$LXC_USER@$LXC_HOST:/etc/systemd/system/stocktracker-backend.service"
echo -e "${GREEN}✓ Service file copied${NC}"

# Step 8: Set up user and permissions
echo -e "\n${YELLOW}[8/9] Setting up user and permissions...${NC}"
ssh -p "$LXC_PORT" "$LXC_USER@$LXC_HOST" bash << 'EOF'
    # Create stocktracker user if not exists
    if ! id -u stocktracker > /dev/null 2>&1; then
        useradd -r -s /bin/false -d /opt/stocktracker stocktracker
        echo "Created stocktracker user"
    else
        echo "User stocktracker already exists"
    fi

    # Set permissions
    chown -R stocktracker:stocktracker /opt/stocktracker
    chmod 755 /opt/stocktracker/stocktracker.jar
    chmod 600 /opt/stocktracker/.env

    echo "Permissions set"
EOF
echo -e "${GREEN}✓ User and permissions configured${NC}"

# Step 9: Enable and start service
echo -e "\n${YELLOW}[9/9] Starting service...${NC}"
ssh -p "$LXC_PORT" "$LXC_USER@$LXC_HOST" bash << 'EOF'
    # Reload systemd daemon
    systemctl daemon-reload

    # Enable service to start on boot
    systemctl enable stocktracker-backend

    # Restart service
    systemctl restart stocktracker-backend

    # Wait a moment for service to start
    sleep 2

    echo "Service status:"
    systemctl status stocktracker-backend --no-pager || true

    echo -e "\nRecent logs:"
    journalctl -u stocktracker-backend -n 20 --no-pager
EOF

echo -e "\n${GREEN}=== Deployment Complete! ===${NC}"
echo -e "${BLUE}Service is running on: http://$LXC_HOST:8080${NC}"
echo -e "\nUseful commands:"
echo -e "  View logs:       ${YELLOW}ssh $LXC_USER@$LXC_HOST 'journalctl -u stocktracker-backend -f'${NC}"
echo -e "  Check status:    ${YELLOW}ssh $LXC_USER@$LXC_HOST 'systemctl status stocktracker-backend'${NC}"
echo -e "  Restart service: ${YELLOW}ssh $LXC_USER@$LXC_HOST 'systemctl restart stocktracker-backend'${NC}"
echo
echo -e "${YELLOW}Next steps:${NC}"
echo -e "  1. Configure Nginx Proxy Manager to proxy to http://$LXC_HOST:8080"
echo -e "  2. Test application access via your proxy hostname"
echo -e "  3. Monitor logs for any issues"
