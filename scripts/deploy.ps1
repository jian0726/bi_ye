# ============================================================
# 简柚个人博客 —— 一键部署脚本（Windows / PowerShell）
# 用法（在仓库根目录执行）：
#   powershell -ExecutionPolicy Bypass -File scripts\deploy.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\deploy.ps1 -Rebuild   # 强制重建镜像
#   powershell -ExecutionPolicy Bypass -File scripts\deploy.ps1 -Down      # 停服
#
# 关键约定（勿改）：
#   1) 宿主机 3306/6379 已被本机服务占用，故映射 3307/6380；
#   2) 8080 被其他项目 Jenkins 占用，故后端对外映射 8081；
#   3) 端口覆盖必须【每条 compose 命令都写全】—— --force-recreate 不继承环境变量，
#      漏写会回退默认端口，且会连带重建 depends_on 的下游容器。
# ============================================================
param(
    [switch]$Rebuild,
    [switch]$Down
)

$ErrorActionPreference = "Stop"

# ---- 端口配置（按本机实际情况调整）----
$BACKEND_PORT  = "8081"
$FRONTEND_PORT = "5174"

Set-Location -Path (Split-Path -Parent $PSScriptRoot)

# 把所有端口写进环境变量，后续每条 compose 命令都继承（前提：同一条命令内 export）
$env:BACKEND_PORT  = $BACKEND_PORT
$env:FRONTEND_PORT = $FRONTEND_PORT

Write-Host "=== 简柚博客一键部署 ===" -ForegroundColor Cyan
Write-Host "后端: http://localhost:$BACKEND_PORT" -ForegroundColor Yellow
Write-Host "前端: http://localhost:$FRONTEND_PORT" -ForegroundColor Yellow
Write-Host "MinIO 控制台: http://localhost:9001 (minioadmin / minioadmin2026)" -ForegroundColor Yellow
Write-Host ""

if ($Down) {
    Write-Host ">>> 停止并移除容器（保留数据卷）..." -ForegroundColor Green
    docker compose down
    Write-Host "已停服。" -ForegroundColor Green
    exit 0
}

if ($Rebuild) {
    Write-Host ">>> 重新构建镜像（容器内 Maven/npm 编译，耗时较长）..." -ForegroundColor Green
    docker compose build
}

Write-Host ">>> 启动全部服务..." -ForegroundColor Green
docker compose up -d

Write-Host ""
Write-Host ">>> 等待服务就绪..." -ForegroundColor Green
$ok = $false
for ($i = 1; $i -le 30; $i++) {
    Start-Sleep -Seconds 2
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:$BACKEND_PORT/api/portal/site/stats" -UseBasicParsing -TimeoutSec 3
        if ($r.StatusCode -eq 200) { $ok = $true; break }
    } catch { }
    Write-Host "  等待中... ($i/30)"
}

if ($ok) {
    Write-Host ""
    Write-Host "=== 部署完成，后端已就绪 ===" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "=== 后端未在预期时间内就绪，请查看日志：docker logs jianyou-backend ===" -ForegroundColor Red
}

Write-Host ""
Write-Host "--- 容器状态 ---" -ForegroundColor Cyan
docker compose ps
