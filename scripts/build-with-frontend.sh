#!/bin/bash
set -e  # Exit on any error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get the project root directory (parent of scripts)
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo -e "${GREEN}=== Building Stock Tracker Full Stack Application ===${NC}"
echo "Project root: $PROJECT_ROOT"

# Step 1: Build frontend
echo -e "\n${YELLOW}[1/4] Building frontend...${NC}"
cd "$PROJECT_ROOT/frontend"
npm run build
if [ $? -ne 0 ]; then
    echo -e "${RED}Frontend build failed!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Frontend build complete${NC}"

# Step 2: Copy frontend dist to backend static resources
echo -e "\n${YELLOW}[2/4] Copying frontend to backend static resources...${NC}"
BACKEND_STATIC="$PROJECT_ROOT/backend/src/main/resources/static"
rm -rf "$BACKEND_STATIC"/*
mkdir -p "$BACKEND_STATIC"
cp -r dist/* "$BACKEND_STATIC/"
echo -e "${GREEN}✓ Frontend copied to backend${NC}"

# Step 3: Build backend with frontend included
echo -e "\n${YELLOW}[3/4] Building backend JAR with embedded frontend...${NC}"
cd "$PROJECT_ROOT/backend"
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}Backend build failed!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Backend build complete${NC}"

# Step 4: Show build artifacts
echo -e "\n${YELLOW}[4/4] Build artifacts:${NC}"
JAR_FILE=$(ls -1 "$PROJECT_ROOT/backend/target"/stocktracker*.jar 2>/dev/null | grep -v "original" | head -1)
if [ -n "$JAR_FILE" ]; then
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    echo -e "${GREEN}✓ JAR file: $JAR_FILE${NC}"
    echo -e "  Size: $JAR_SIZE"
else
    echo -e "${RED}✗ JAR file not found!${NC}"
    exit 1
fi

echo -e "\n${GREEN}=== Build Complete! ===${NC}"
echo -e "You can now run: ${YELLOW}./scripts/deploy.sh${NC} to deploy to your LXC container"
