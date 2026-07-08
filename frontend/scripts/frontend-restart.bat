@echo off
chcp 65001 >nul
REM ============================================================================
REM frontend-restart.bat — Emergency restart for the Vite dev server
REM ============================================================================
REM Double-click this when:
REM   - http://localhost:5173 shows 404 or is unreachable
REM   - The page loads but is completely white
REM   - npm run dev fails with "port already in use"
REM
REM It does a complete nuclear cleanup, then restarts from scratch.
REM ============================================================================

echo ========================================
echo   Club Management - Emergency Restart
echo ========================================
echo.

REM ── Phase 1: Kill ALL processes on port 5173 ────────────────────────────
echo [1/4] Killing all processes on port 5173...

for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173 ^| findstr LISTENING') do (
    echo   Killing PID %%a...
    taskkill /F /PID %%a >nul 2>&1
)

REM PowerShell backup scan
powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue; Write-Host '  Stopped PID' $_.OwningProcess }"

REM Wait for processes to die
timeout /t 2 /nobreak >nul

REM ── Phase 2: Nuclear option — kill all node.exe ─────────────────────────
echo.
echo [2/4] Nuclear cleanup — killing all node.exe...

powershell -NoProfile -Command "Get-Process -Name node -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue"

timeout /t 2 /nobreak >nul

REM ── Phase 3: Double-confirm port is free ────────────────────────────────
echo.
echo [3/4] Verifying port 5173 is free...

set PORT_FREE=1
netstat -ano | findstr :5173 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    set PORT_FREE=0
    echo   WARNING: Port 5173 is STILL busy!
    echo   Close all IDEA/terminal windows and try again.
    pause
    exit /b 1
) else (
    echo   Port 5173 is free - ready to start
)

REM ── Phase 4: Start Vite from the correct directory ──────────────────────
echo.
echo [4/4] Starting Vite from D:\club-management\frontend...

cd /d D:\club-management\frontend

echo   Running: npm run dev
echo   (predev kill-port.js will run first to verify clean state)
echo.

start "" http://localhost:5173

npm run dev

pause
