# ============================================
# 社团管理系统 - 智能启动脚本 (永久方案)
# 双击运行，或右键"使用PowerShell运行"
# ============================================
param(
    [switch]$BackendOnly,
    [switch]$FrontendOnly
)

$ErrorActionPreference = "Stop"
$RootDir = "D:\club-management"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  社团管理系统 - 智能启动" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ==========================================
# 1. 清理端口
# ==========================================
function Kill-Port {
    param([int]$Port, [string]$Name)

    # 方法1: Get-NetTCPConnection (可能不显示全部IP绑定)
    $pids1 = @(Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
               Where-Object { $_.State -eq 'Listen' } |
               Select-Object -ExpandProperty OwningProcess -Unique)

    # 方法2: netstat (更可靠，能看到所有IP绑定)
    $netstatLines = & netstat -ano 2>$null | Select-String ":$Port\s"
    $pids2 = @($netstatLines | ForEach-Object {
        $line = $_ -replace '\s+', ' '
        $parts = $line.Split(' ')
        if ($parts.Count -gt 4) { [int]$parts[-1] }
    } | Where-Object { $_ -gt 0 } | Sort-Object -Unique)

    # 合并两个来源的所有PID
    $allPids = @($pids1 + $pids2 | Sort-Object -Unique)

    if ($allPids.Count -gt 0) {
        Write-Host "[清理] 端口 ${Port} 发现 $($allPids.Count) 个关联进程，全部关闭..." -ForegroundColor Yellow
        foreach ($procId in $allPids) {
            $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
            if ($proc) {
                Write-Host "        关闭 $($proc.ProcessName) (PID: $procId)" -ForegroundColor DarkYellow
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                # 确认已杀掉
                Start-Sleep -Milliseconds 500
            }
        }

        # 等待端口真正释放（最多10秒）
        $retries = 0
        do {
            Start-Sleep -Seconds 1
            $stillListening = @(Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
                               Where-Object { $_.State -eq 'Listen' })
            if ($stillListening.Count -eq 0) { break }
            $retries++
        } while ($retries -lt 10)

        if ($retries -ge 10) {
            Write-Host "[清理] ⚠ 端口 ${Port} 未完全释放，继续尝试..." -ForegroundColor Red
            # 最后手段：taskkill /F /IM
            & taskkill //F //IM "node.exe" 2>$null | Out-Null
            & taskkill //F //IM "java.exe" 2>$null | Out-Null
            Start-Sleep -Seconds 3
        }

        Write-Host "[清理] 端口 ${Port} 已释放 ✓" -ForegroundColor Green
    } else {
        Write-Host "[清理] 端口 ${Port} 空闲 ✓" -ForegroundColor Gray
    }
}

# ==========================================
# 2. 启动后端
# ==========================================
function Start-Backend {
    Write-Host ""
    Write-Host ">>> 启动后端 Spring Boot (端口 8080)..." -ForegroundColor Cyan
    Kill-Port -Port 8080 -Name "Backend"

    $backendDir = "$RootDir\backend"
    if (-not (Test-Path $backendDir)) {
        Write-Host "[错误] 后端目录不存在: $backendDir" -ForegroundColor Red
        return $false
    }

    # 检查 Java
    $java = Get-Command java -ErrorAction SilentlyContinue
    if (-not $java) {
        Write-Host "[错误] 未找到 Java，请确认已安装 JDK 17" -ForegroundColor Red
        return $false
    }

    # 启动后端
    $proc = Start-Process -FilePath "cmd" -ArgumentList "/c cd /d $backendDir && mvn spring-boot:run" -WindowStyle Minimized -PassThru

    Write-Host "[后端] 正在启动 (PID: $($proc.Id))，等待就绪..." -ForegroundColor Yellow

    # 等待后端就绪（最多120秒）
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 2
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -ContentType "application/json" -Body '{}' -TimeoutSec 2 -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 403 -or $response.StatusCode -eq 200) {
                $ready = $true
                break
            }
        } catch {
            # 还没就绪，继续等
        }
    }

    if ($ready) {
        Write-Host "[后端] 启动成功！http://localhost:8080" -ForegroundColor Green
        return $true
    } else {
        Write-Host "[后端] 启动超时，请检查后端日志" -ForegroundColor Red
        return $false
    }
}

# ==========================================
# 3. 启动前端
# ==========================================
function Start-Frontend {
    Write-Host ""
    Write-Host ">>> 启动前端 Vite (端口 5173)..." -ForegroundColor Cyan
    Kill-Port -Port 5173 -Name "Frontend"

    $frontendDir = "$RootDir\frontend"
    if (-not (Test-Path $frontendDir)) {
        Write-Host "[错误] 前端目录不存在: $frontendDir" -ForegroundColor Red
        return $false
    }

    # 检查 node_modules
    if (-not (Test-Path "$frontendDir\node_modules")) {
        Write-Host "[前端] 正在安装依赖..." -ForegroundColor Yellow
        Start-Process -FilePath "cmd" -ArgumentList "/c cd /d $frontendDir && npm install" -Wait -NoNewWindow
    }

    # Use "npm run dev" so the predev hook (kill-port.js) runs automatically
    # --host is no longer needed: vite.config.js sets host:'0.0.0.0'
    $proc = Start-Process -FilePath "cmd" -ArgumentList "/c cd /d $frontendDir && npm run dev" -WindowStyle Minimized -PassThru

    Write-Host "[前端] 正在启动 (PID: $($proc.Id))，等待就绪..." -ForegroundColor Yellow

    # 等待前端就绪（最多60秒）
    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:5173" -TimeoutSec 2 -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 200) {
                $ready = $true
                break
            }
        } catch {
            # 还没就绪
        }
    }

    if ($ready) {
        Write-Host "[前端] 启动成功！http://localhost:5173" -ForegroundColor Green
        return $true
    } else {
        Write-Host "[前端] 启动超时，请检查前端日志" -ForegroundColor Red
        return $false
    }
}

# ==========================================
# 4. 主流程
# ==========================================
$backendOk = $true
$frontendOk = $true

if (-not $FrontendOnly) {
    $backendOk = Start-Backend
}

if (-not $BackendOnly) {
    $frontendOk = Start-Frontend
}

# ==========================================
# 5. 结果
# ==========================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($backendOk -and $frontendOk) {
    Write-Host "  ✓ 全部启动成功！" -ForegroundColor Green
} elseif ($backendOk) {
    Write-Host "  △ 后端启动成功，前端启动失败" -ForegroundColor Yellow
} elseif ($frontendOk) {
    Write-Host "  △ 前端启动成功，后端启动失败" -ForegroundColor Yellow
} else {
    Write-Host "  ✗ 启动失败，请检查配置" -ForegroundColor Red
}
Write-Host ""
Write-Host "  前端: http://localhost:5173" -ForegroundColor Green
Write-Host "  后端: http://localhost:8080" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

# ==========================================
# 6. 双IP绑定检测（防止白屏）
# ==========================================
Write-Host ""
Write-Host ">>> 安全检测：检查端口多重绑定..." -ForegroundColor Cyan
$port5173Listeners = @(Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue |
                      Where-Object { $_.State -eq 'Listen' })
if ($port5173Listeners.Count -gt 2) {
    Write-Host "[警告] 检测到端口 5173 有 $($port5173Listeners.Count) 个监听！可能导致白屏" -ForegroundColor Red
    $port5173Listeners | ForEach-Object {
        Write-Host "        $($_.LocalAddress):5173  PID=$($_.OwningProcess)" -ForegroundColor Red
    }
    Write-Host "[建议] 关闭浏览器后运行 frontend-restart.bat" -ForegroundColor Yellow
} elseif ($port5173Listeners.Count -eq 0) {
    Write-Host "[警告] 前端端口 5173 未检测到监听进程" -ForegroundColor Red
} else {
    Write-Host "[检测] 前端端口正常 (监听数: $($port5173Listeners.Count))" -ForegroundColor Green
}

# 自动打开浏览器
Start-Process "http://localhost:5173"

Write-Host ""
Write-Host "按任意键退出（不影响服务运行）..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
