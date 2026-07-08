@echo off
chcp 65001 >nul
title 社团管理系统 - 一键启动

echo ========================================
echo   社团管理系统 - 启动中...
echo ========================================
echo.

REM ===== 启动后端 (Spring Boot 8080) =====
echo [1/2] 启动后端 Spring Boot (端口 8080)...
start "Backend-8080" cmd /k "cd /d D:\club-management\backend && mvn spring-boot:run"

REM 等待后端启动
echo        等待后端初始化 (约15秒)...
timeout /t 8 /nobreak >nul

REM ===== 启动前端 (Vite 5173) =====
echo [2/2] 启动前端 Vite (端口 5173)...
start "Frontend-5173" cmd /k "cd /d D:\club-management\frontend && npm run dev"

echo.
echo ========================================
echo   启动完成！
echo   后端: http://localhost:8080
echo   前端: http://localhost:5173
echo ========================================
echo.
echo   按任意键退出此窗口（不影响前后端运行）
pause >nul
