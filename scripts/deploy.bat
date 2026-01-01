@echo off
setlocal enabledelayedexpansion

REM Stock Tracker Deployment Script for Windows
REM ============================================
REM
REM Prerequisites:
REM   - SSH client installed (Git for Windows, Windows OpenSSH, or WSL)
REM   - SSH access configured to LXC container
REM
REM Usage:
REM   set LXC_HOST=192.168.1.100
REM   scripts\deploy.bat
REM
REM Optional environment variables:
REM   LXC_HOST - IP or hostname of LXC container (required)
REM   LXC_USER - SSH username (default: root)
REM   LXC_PORT - SSH port (default: 22)
REM ============================================

REM Configuration - can be overridden by environment variables
if not defined LXC_HOST set "LXC_HOST=192.168.1.100"
if not defined LXC_USER set "LXC_USER=root"
if not defined LXC_PORT set "LXC_PORT=22"

REM Get the project root directory (parent of scripts)
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "SCRIPTS_DIR=%PROJECT_ROOT%\scripts"
cd /d "%PROJECT_ROOT%"

echo ========================================
echo Stock Tracker Deployment Script
echo ========================================
echo Target: %LXC_USER%@%LXC_HOST%:%LXC_PORT%
echo.

REM Check if SSH is available
where ssh >nul 2>&1
if errorlevel 1 (
    echo [ERROR] SSH client not found!
    echo.
    echo Please install one of the following:
    echo   - Git for Windows (https://git-scm.com/download/win^)
    echo   - Windows OpenSSH (Settings ^> Apps ^> Optional Features^)
    echo   - WSL (Windows Subsystem for Linux^)
    exit /b 1
)

REM Check if SCP is available
where scp >nul 2>&1
if errorlevel 1 (
    echo [ERROR] SCP client not found!
    echo Please install SSH tools (see above^)
    exit /b 1
)

REM Step 1: Build the application
echo ========================================
echo [1/9] Building application...
echo ========================================
call "%SCRIPTS_DIR%\build-with-frontend.bat"
if errorlevel 1 (
    echo [ERROR] Build failed!
    exit /b 1
)
echo [SUCCESS] Build complete
echo.

REM Find the JAR file
set "JAR_FILE="
for %%F in ("%PROJECT_ROOT%\backend\target\stocktracker*.jar") do (
    set "JAR_NAME=%%~nxF"
    if not "!JAR_NAME:~0,8!"=="original" (
        set "JAR_FILE=%%F"
        goto :jar_found
    )
)

echo [ERROR] JAR file not found!
exit /b 1

:jar_found
echo Found JAR: %JAR_FILE%
echo.

REM Step 2: Check if .env file exists
echo ========================================
echo [2/9] Checking environment configuration...
echo ========================================
if not exist "%SCRIPTS_DIR%\.env.production" (
    echo [ERROR] .env.production not found!
    echo.
    echo Please create %SCRIPTS_DIR%\.env.production from the template:
    echo   copy %SCRIPTS_DIR%\.env.production.template %SCRIPTS_DIR%\.env.production
    echo   Then edit .env.production with your actual values
    exit /b 1
)
echo [SUCCESS] Environment file found
echo.

REM Step 3: Test SSH connection
echo ========================================
echo [3/9] Testing SSH connection...
echo ========================================
ssh -p %LXC_PORT% %LXC_USER%@%LXC_HOST% "echo SSH connection successful" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Cannot connect to %LXC_USER%@%LXC_HOST%:%LXC_PORT%
    echo.
    echo Please check:
    echo   1. SSH is running on the LXC container
    echo   2. SSH keys are set up (or you'll be prompted for password^)
    echo   3. Host and port are correct
    exit /b 1
)
echo [SUCCESS] SSH connection successful
echo.

REM Step 4: Create directory structure on LXC
echo ========================================
echo [4/9] Creating directory structure on LXC...
echo ========================================
ssh -p %LXC_PORT% %LXC_USER%@%LXC_HOST% "mkdir -p /opt/stocktracker"
if errorlevel 1 (
    echo [ERROR] Failed to create directory
    exit /b 1
)
echo [SUCCESS] Directory created
echo.

REM Step 5: Copy JAR file
echo ========================================
echo [5/9] Copying JAR file to LXC...
echo ========================================
scp -P %LXC_PORT% "%JAR_FILE%" %LXC_USER%@%LXC_HOST%:/opt/stocktracker/stocktracker.jar
if errorlevel 1 (
    echo [ERROR] Failed to copy JAR file
    exit /b 1
)
echo [SUCCESS] JAR file copied
echo.

REM Step 6: Copy environment file
echo ========================================
echo [6/9] Copying environment file...
echo ========================================
scp -P %LXC_PORT% "%SCRIPTS_DIR%\.env.production" %LXC_USER%@%LXC_HOST%:/opt/stocktracker/.env
if errorlevel 1 (
    echo [ERROR] Failed to copy environment file
    exit /b 1
)
echo [SUCCESS] Environment file copied
echo.

REM Step 7: Copy and install systemd service
echo ========================================
echo [7/9] Installing systemd service...
echo ========================================
scp -P %LXC_PORT% "%SCRIPTS_DIR%\stocktracker-backend.service" %LXC_USER%@%LXC_HOST%:/etc/systemd/system/stocktracker-backend.service
if errorlevel 1 (
    echo [ERROR] Failed to copy service file
    exit /b 1
)
echo [SUCCESS] Service file copied
echo.

REM Step 8: Set up user and permissions
echo ========================================
echo [8/9] Setting up user and permissions...
echo ========================================
ssh -p %LXC_PORT% %LXC_USER%@%LXC_HOST% "id -u stocktracker 2>/dev/null || useradd -r -s /bin/false -d /opt/stocktracker stocktracker; chown -R stocktracker:stocktracker /opt/stocktracker; chmod 755 /opt/stocktracker/stocktracker.jar; chmod 600 /opt/stocktracker/.env"
if errorlevel 1 (
    echo [ERROR] Failed to set up user and permissions
    exit /b 1
)
echo [SUCCESS] User and permissions configured
echo.

REM Step 9: Enable and start service
echo ========================================
echo [9/9] Starting service...
echo ========================================
ssh -p %LXC_PORT% %LXC_USER%@%LXC_HOST% "systemctl daemon-reload; systemctl enable stocktracker-backend; systemctl restart stocktracker-backend"
if errorlevel 1 (
    echo [ERROR] Failed to start service
    exit /b 1
)

REM Wait for service to start
echo Waiting for service to start...
timeout /t 3 /nobreak >nul

REM Show service status
echo.
echo Service status:
ssh -p %LXC_PORT% %LXC_USER%@%LXC_HOST% "systemctl status stocktracker-backend --no-pager || true"

echo.
echo Recent logs:
ssh -p %LXC_PORT% %LXC_USER%@%LXC_HOST% "journalctl -u stocktracker-backend -n 20 --no-pager || true"

echo.
echo ========================================
echo Deployment Complete!
echo ========================================
echo Service is running on: http://%LXC_HOST%:8080
echo.
echo Useful commands:
echo   View logs:       ssh %LXC_USER%@%LXC_HOST% "journalctl -u stocktracker-backend -f"
echo   Check status:    ssh %LXC_USER%@%LXC_HOST% "systemctl status stocktracker-backend"
echo   Restart service: ssh %LXC_USER%@%LXC_HOST% "systemctl restart stocktracker-backend"
echo.
echo Next steps:
echo   1. Configure Nginx Proxy Manager to proxy to http://%LXC_HOST%:8080
echo   2. Test application access via your proxy hostname
echo   3. Monitor logs for any issues

endlocal
exit /b 0
