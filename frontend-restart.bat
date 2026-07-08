@echo off
chcp 65001 >nul
title 前端修复 — 端口5173清理重启

echo ========================================
echo   前端修复工具（彻底版）
echo ========================================
echo.

echo [1/4] 查找所有占用 5173 端口的进程...
set FOUND=0
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173"') do (
    echo        发现 PID: %%a，正在关闭...
    taskkill /PID %%a /F >nul 2>&1
    set FOUND=1
)

if %FOUND%==0 (
    echo        未发现占用进程
)

echo.
echo [2/4] 额外清理所有 node.exe 进程（确保干净）...
taskkill //F //IM "node.exe" >nul 2>&1
echo        完成

echo.
echo [3/4] 等待端口完全释放...
timeout /t 3 /nobreak >nul

REM 二次确认
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173.*LISTENING"') do (
    echo        ⚠ 端口仍被 PID %%a 占用，强制清理...
    taskkill /PID %%a /F >nul 2>&1
    timeout /t 2 /nobreak >nul
)

echo.
echo [4/4] 在正确目录启动前端...
cd /d D:\club-management\frontend
echo        目录: %cd%
echo        启动命令: npm run dev
start "Frontend-5173" cmd /c "cd /d D:\club-management\frontend && npm run dev"

echo.
echo ========================================
echo   修复完成！5秒后打开浏览器...
echo   如仍白屏，请按 Ctrl+F5 强制刷新
echo ========================================
timeout /t 5 /nobreak >nul
start http://localhost:5173

echo.
echo   按任意键关闭此窗口...
pause >nul
