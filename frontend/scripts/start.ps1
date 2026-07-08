# ============================================================================
# start.ps1 — Smart launcher for the club-management Vite dev server
# ============================================================================
# Usage: Right-click → "Run with PowerShell" (or from terminal)
#
# What it does:
#   1. Kills any zombie Vite processes on port 5173 (dual-method scan)
#   2. Waits for port release
#   3. Runs `npm run dev` (which triggers kill-port.js predev + vite)
#   4. Waits for Vite to be healthy (HTTP 200)
#   5. Checks for dual-IP binding (0.0.0.0 + 127.0.0.1 both on 5173 = BAD)
#   6. Opens browser to http://localhost:5173
# ============================================================================

$ErrorActionPreference = "Stop"
$PORT = 5173
$FRONTEND_DIR = "D:\club-management\frontend"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Club Management - Frontend Launcher" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ── Step 1: Kill any existing processes on port 5173 ─────────────────────

Write-Host "[1/5] Checking for existing processes on port $PORT..." -ForegroundColor Yellow

$killed = $false

# Method A: netstat
$netstatPids = @()
$netstatOutput = & cmd /c "netstat -ano | findstr :$PORT" 2>$null
if ($netstatOutput) {
    foreach ($line in $netstatOutput) {
        if ($line -match 'LISTENING\s+(\d+)$') {
            $netstatPids += $Matches[1]
        }
    }
}

# Method B: Get-NetTCPConnection
$tcpPids = @()
try {
    $tcpPids = (Get-NetTCPConnection -LocalPort $PORT -ErrorAction SilentlyContinue |
                Where-Object { $_.State -eq 'Listen' } |
                Select-Object -ExpandProperty OwningProcess -Unique)
} catch { }

$allPids = ($netstatPids + $tcpPids) | Sort-Object -Unique

if ($allPids.Count -gt 0) {
    Write-Host "  Found process(es) on port $PORT : $($allPids -join ', ')" -ForegroundColor Red
    foreach ($pidToKill in $allPids) {
        try {
            Stop-Process -Id $pidToKill -Force -ErrorAction Stop
            Write-Host "  Killed PID $pidToKill ✓" -ForegroundColor Green
            $killed = $true
        } catch {
            Write-Host "  Failed to kill PID $pidToKill (may already be gone)" -ForegroundColor DarkYellow
        }
    }
} else {
    Write-Host "  Port $PORT is free." -ForegroundColor Green
}

# ── Step 2: Wait for port release ─────────────────────────────────────────

if ($killed) {
    Write-Host "[2/5] Waiting for port $PORT to be released..." -ForegroundColor Yellow
    for ($i = 0; $i -lt 20; $i++) {
        $stillListening = & cmd /c "netstat -ano | findstr :$PORT | findstr LISTENING" 2>$null
        if (-not $stillListening) {
            Write-Host "  Port released ✓" -ForegroundColor Green
            break
        }
        Start-Sleep -Milliseconds 500
    }
    # Double-check: nuclear option if still busy
    $stillListening = & cmd /c "netstat -ano | findstr :$PORT | findstr LISTENING" 2>$null
    if ($stillListening) {
        Write-Host "  Port still busy. Nuclear option: killing all node.exe..." -ForegroundColor Red
        Get-Process -Name node -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
}

# ── Step 3: Start Vite ────────────────────────────────────────────────────

Write-Host "[3/5] Starting Vite dev server..." -ForegroundColor Yellow
Set-Location $FRONTEND_DIR

# Start npm run dev in background
$npmProcess = Start-Process -FilePath "npm" -ArgumentList "run","dev" -PassThru -NoNewWindow -WorkingDirectory $FRONTEND_DIR

# ── Step 4: Wait for Vite to be healthy ────────────────────────────────────

Write-Host "[4/5] Waiting for Vite to be ready (HTTP 200)..." -ForegroundColor Yellow
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:$PORT/" -TimeoutSec 2 -UseBasicParsing -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "  Vite is ready! (HTTP 200) ✓" -ForegroundColor Green
            $ready = $true
            break
        }
    } catch {
        # Not ready yet
    }
    Start-Sleep -Seconds 1
}

if (-not $ready) {
    Write-Host "  ⚠ Vite didn't respond with 200 within 30s. It may still be starting..." -ForegroundColor DarkYellow
}

# ── Step 5: Dual-IP binding check ─────────────────────────────────────────

Write-Host "[5/5] Checking for dual-IP binding (Windows anti-pattern)..." -ForegroundColor Yellow
try {
    $connections = Get-NetTCPConnection -LocalPort $PORT -ErrorAction SilentlyContinue |
                   Where-Object { $_.State -eq 'Listen' }
    $addresses = $connections | Select-Object -ExpandProperty LocalAddress -Unique

    if ($addresses.Count -gt 1) {
        Write-Host "  ⚠ WARNING: Multiple IP addresses bound to port $PORT !" -ForegroundColor Red
        Write-Host "    Addresses: $($addresses -join ', ')" -ForegroundColor Red
        Write-Host "    This causes random 404/white-screen. Restart if issues occur." -ForegroundColor Red
    } else {
        Write-Host "  Single binding: $($addresses -join '') ✓" -ForegroundColor Green
    }
} catch {
    Write-Host "  Could not check bindings (non-critical)" -ForegroundColor DarkYellow
}

# ── Open browser ───────────────────────────────────────────────────────────

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Opening http://localhost:$PORT/ ..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Start-Process "http://localhost:$PORT/"

Write-Host ""
Write-Host "Press any key to close this window (Vite runs in background)..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
