@echo off
REM Quick Start Guide for Gateway Studio (Windows)

echo.
echo ==========================================
echo Gateway Studio - Docker Quick Start
echo ==========================================
echo.

REM Check if Docker is running
docker --version >nul 2>&1
if errorlevel 1 (
    echo.
    echo ERROR: Docker is not installed or not in PATH
    echo.
    pause
    exit /b 1
)

echo Docker found
echo.

REM Change to script directory
cd /d "%~dp0"

echo.
echo Building and starting containers...
echo (First build: 2-3 minutes ^| Subsequent: 30 seconds)
echo.

REM Build and start
docker compose up --build

echo.
echo ==========================================
echo Gateway Studio is running!
echo ==========================================
echo.
echo Open your browser:
echo   - Frontend UI: http://localhost:3000
echo   - Backend API: http://localhost:8080
echo.
echo To view logs:
echo   docker compose logs -f
echo.
echo To stop:
echo   Press Ctrl+C (graceful shutdown)
echo   Or in another terminal:
echo   docker compose down
echo.
pause

