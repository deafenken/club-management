# =====================================================
# 社团管理系统 — 一键部署脚本
# 使用: 右键 deploy.ps1 → 使用 PowerShell 运行
# =====================================================
$ErrorActionPreference = "Stop"
$SERVER = "root@123.57.229.185"
$FRONTEND_LOCAL = "D:\club-management\frontend\dist\*"
$FRONTEND_REMOTE = "/opt/club-management/frontend/dist/"
$BACKEND_LOCAL = "D:\club-management\backend\target\club-management-1.0.0.jar"
$BACKEND_REMOTE = "/opt/club-management/backend/target/"

Write-Host "===== 1. 构建前端 =====" -ForegroundColor Cyan
Set-Location D:\club-management\frontend
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force node_modules\.vite -ErrorAction SilentlyContinue
npx vite build
if ($LASTEXITCODE -ne 0) { throw "前端构建失败" }
Write-Host "前端构建完成" -ForegroundColor Green

Write-Host "===== 2. 构建后端 =====" -ForegroundColor Cyan
Set-Location D:\club-management\backend
mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw "后端构建失败" }
Write-Host "后端构建完成" -ForegroundColor Green

Write-Host "===== 3. 停止后端服务 =====" -ForegroundColor Cyan
ssh $SERVER "systemctl stop club-backend.service"
Write-Host "后端已停止" -ForegroundColor Green

Write-Host "===== 4. 彻底清除服务器旧文件 =====" -ForegroundColor Cyan
ssh $SERVER "rm -rf /opt/club-management/frontend/dist/*"
ssh $SERVER "ls /opt/club-management/frontend/dist/ | wc -l"
Write-Host "旧文件已清除" -ForegroundColor Green

Write-Host "===== 5. 上传前端 =====" -ForegroundColor Cyan
scp -r $FRONTEND_LOCAL "${SERVER}:${FRONTEND_REMOTE}"
Write-Host "前端上传完成" -ForegroundColor Green

Write-Host "===== 6. 上传后端 JAR =====" -ForegroundColor Cyan
scp $BACKEND_LOCAL "${SERVER}:${BACKEND_REMOTE}"
Write-Host "后端上传完成" -ForegroundColor Green

Write-Host "===== 7. 启动服务 =====" -ForegroundColor Cyan
ssh $SERVER "systemctl start club-backend.service"
ssh $SERVER "systemctl restart nginx"
Write-Host "服务已启动" -ForegroundColor Green

Write-Host "===== 8. 验证 =====" -ForegroundColor Cyan
Start-Sleep -Seconds 3
$frontend = ssh $SERVER "curl -so /dev/null -w '%{http_code}' http://localhost/"
$login = ssh $SERVER "curl -so /dev/null -w '%{http_code}' -X POST http://localhost:8080/api/user/login -H 'Content-Type: application/json' -d '{\"username\":\"admin\",\"password\":\"123456\"}'"
Write-Host "前端: $frontend | 登录API: $login" -ForegroundColor $(if ($frontend -eq "200" -and $login -eq "200") { "Green" } else { "Red" })

Write-Host "`n===== 部署完成! =====" -ForegroundColor Green
Write-Host "访问: http://123.57.229.185" -ForegroundColor Yellow
Write-Host "如果页面没更新,请 Ctrl+Shift+Delete 清除浏览器缓存" -ForegroundColor Yellow
