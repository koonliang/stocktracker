@echo off
setlocal enabledelayedexpansion

REM Stock Tracker Full Stack Build Script for Windows
REM ===================================================

REM Get the project root directory (parent of scripts)
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
cd /d "%PROJECT_ROOT%"

echo ========================================
echo Building Stock Tracker Full Stack Application
echo ========================================
echo Project root: %PROJECT_ROOT%
echo.

REM Step 1: Build frontend
echo ========================================
echo [1/4] Building frontend...
echo ========================================
cd /d "%PROJECT_ROOT%\frontend"
call npm run build
if errorlevel 1 (
    echo [ERROR] Frontend build failed!
    exit /b 1
)
echo [SUCCESS] Frontend build complete
echo.

REM Step 2: Copy frontend dist to backend static resources
echo ========================================
echo [2/4] Copying frontend to backend static resources...
echo ========================================
set "BACKEND_STATIC=%PROJECT_ROOT%\backend\src\main\resources\static"

REM Remove existing static files
if exist "%BACKEND_STATIC%" (
    rd /s /q "%BACKEND_STATIC%"
)

REM Create static directory
if not exist "%BACKEND_STATIC%" (
    mkdir "%BACKEND_STATIC%"
)

REM Copy frontend dist to backend static
xcopy /E /I /Y "dist" "%BACKEND_STATIC%"
if errorlevel 1 (
    echo [ERROR] Failed to copy frontend files!
    exit /b 1
)
echo [SUCCESS] Frontend copied to backend
echo.

REM Step 3: Build backend with frontend included
echo ========================================
echo [3/4] Building backend JAR with embedded frontend...
echo ========================================
cd /d "%PROJECT_ROOT%\backend"
call mvn clean package -DskipTests
REM Remove static folder
if exist "%BACKEND_STATIC%" (
    rmdir /s /q "%BACKEND_STATIC%"
)
if errorlevel 1 (
    echo [ERROR] Backend build failed!
    exit /b 1
)
echo [SUCCESS] Backend build complete
echo.

REM Step 4: Show build artifacts
echo ========================================
echo [4/4] Build artifacts:
echo ========================================

REM Find the JAR file (exclude original)
for %%F in ("%PROJECT_ROOT%\backend\target\stocktracker*.jar") do (
    set "JAR_FILE=%%F"
    set "JAR_NAME=%%~nxF"
    if not "!JAR_NAME:~0,8!"=="original" (
        echo [SUCCESS] JAR file: %%F
        for %%A in ("%%F") do echo   Size: %%~zA bytes
        goto :jar_found
    )
)

echo [ERROR] JAR file not found!
exit /b 1

:jar_found
echo.
echo ========================================
echo Build Complete!
echo ========================================
echo You can now run: scripts\deploy.bat to deploy to your LXC container
echo.

endlocal
exit /b 0
