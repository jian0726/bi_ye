# 一键用 ngrok 暴露「Docker 部署」形态（frontend nginx 容器）
# 用法：
#   .\scripts\tunnel-docker.ps1                  # 默认 5174
#   .\scripts\tunnel-docker.ps1 -Port 8081       # 自定义前端端口
#   .\scripts\tunnel-docker.ps1 -Up              # 容器没跑时自动 docker compose up -d
#
# 说明：
#   frontend nginx 容器已把 /api/ 反代到 backend:8080、/files/ 反代到 minio:9000，
#   所以只需暴露前端一个端口，后端与图床不必单独开隧道。

param(
    [int]$Port = 5174,
    [switch]$Up
)

$ErrorActionPreference = "Stop"
. "$PSScriptRoot\_ngrok-precheck.ps1"

Write-Host "`n=== 简柚博客 · ngrok 隧道（Docker 形态）===`n" -ForegroundColor Cyan

# 1) 环境检查
if (-not (Test-NgrokReady)) { exit 1 }

# 2) Docker 是否在跑
$dockerOk = $false
try {
    $null = docker info 2>$null
    $dockerOk = ($LASTEXITCODE -eq 0)
} catch { $dockerOk = $false }

if (-not $dockerOk) {
    Write-Host "[X] Docker 未运行或不可用" -ForegroundColor Red
    Write-Host "    启动 Docker Desktop 后重试" -ForegroundColor Yellow
    exit 1
}
Write-Host "[OK] Docker 可用" -ForegroundColor Green

# 3) 容器状态
$running = docker ps --filter "name=jianyou-frontend" --format "{{.Status}}" 2>$null
if (-not $running) {
    if ($Up) {
        Write-Host "[..] jianyou-frontend 未运行，执行 docker compose up -d" -ForegroundColor Yellow
        $root = Split-Path $PSScriptRoot -Parent
        Push-Location $root
        $env:BACKEND_PORT = "8081"
        $env:FRONTEND_PORT = "$Port"
        try {
            docker compose up -d
            if (-not (Wait-PortListening -Port $Port -TimeoutSec 120)) {
                Write-Host "[X] 120 秒内端口 $Port 仍未就绪，请查看：docker compose logs -f frontend" -ForegroundColor Red
                exit 1
            }
        } finally {
            Pop-Location
        }
    } else {
        Write-Host "[X] jianyou-frontend 容器未运行" -ForegroundColor Red
        Write-Host "    先启动：`$env:BACKEND_PORT=8081; `$env:FRONTEND_PORT=$Port; docker compose up -d" -ForegroundColor Yellow
        Write-Host "    或加参数自动启动：.\scripts\tunnel-docker.ps1 -Up" -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "[OK] jianyou-frontend 运行中：$running" -ForegroundColor Green
}

# 4) 端口连通性
if (-not (Test-PortListening -Port $Port)) {
    Write-Host "[X] 端口 $Port 未监听（容器可能在跑但端口映射不一致）" -ForegroundColor Red
    Write-Host "    核对端口映射：docker port jianyou-frontend" -ForegroundColor Yellow
    exit 1
}
Write-Host "[OK] 端口 $Port 已监听" -ForegroundColor Green

# 5) 起隧道
#    走代理会被 ngrok 免费版拒绝（ERR_NGROK_9009），故先清掉代理环境变量
Clear-NgrokProxyEnv | Out-Null

Write-Host "`n[..] 正在启动 ngrok http $Port" -ForegroundColor Yellow
Write-Host "     按 Ctrl+C 结束隧道`n" -ForegroundColor DarkGray

Start-Process "http://127.0.0.1:4040" -ErrorAction SilentlyContinue

$log = New-TemporaryFile
$failed = $false
try {
    & ngrok http $Port --log stdout 2>&1 | Tee-Object -FilePath $log
    if ($LASTEXITCODE -ne 0) { $failed = $true }
} catch {
    $failed = $true
} finally {
    $text = if (Test-Path $log) { Get-Content $log -Raw -ErrorAction SilentlyContinue } else { "" }
    if ($failed -or $text -match 'ERR_NGROK_') {
        Write-Host "`n=== 隧道建立失败 ===`n" -ForegroundColor Red
        if ($text -match 'ERR_NGROK_9009|http/s proxy is a Pay-as-you-go') {
            Write-Host "[ERR_NGROK_9009] ngrok 免费版不允许走 HTTP 代理" -ForegroundColor Red
            Write-Host "  检查残留：Get-ChildItem Env: | Where-Object Name -match 'proxy'" -ForegroundColor Yellow
            Write-Host "  检查配置：ngrok config edit（确认无 proxy_url）" -ForegroundColor Yellow
        }
        elseif ($text -match 'ERR_NGROK_105|authentication failed') {
            Write-Host "[ERR_NGROK_105] authtoken 无效：ngrok config add-authtoken <新token>" -ForegroundColor Red
        }
        elseif ($text -match 'ERR_NGROK_108|simultaneous') {
            Write-Host "[ERR_NGROK_108] 免费账号仅允许 1 条隧道在线" -ForegroundColor Red
        }
        else {
            Write-Host "  未识别错误，原始输出：" -ForegroundColor Yellow
        }
        if ($text) {
            ($text -split "`r?`n") | Where-Object { $_ -match 'ERR_NGROK' } | Select-Object -First 3 |
                ForEach-Object { Write-Host "  | $_" -ForegroundColor DarkGray }
        }
        Write-Host ""
    }
    Remove-Item $log -Force -ErrorAction SilentlyContinue
}
