# 一键用 ngrok 暴露「项目直跑」形态（Vite dev server）
# 用法：
#   .\scripts\tunnel-dev.ps1              # 默认 5173
#   .\scripts\tunnel-dev.ps1 -Port 5174   # 自定义端口
#   .\scripts\tunnel-dev.ps1 -StartDev    # 端口未监听时自动起 vite dev
#
# 前置：vite.config.ts 已设置 host: '0.0.0.0' + allowedHosts: true

param(
    [int]$Port = 5173,
    [switch]$StartDev
)

$ErrorActionPreference = "Stop"
. "$PSScriptRoot\_ngrok-precheck.ps1"

Write-Host "`n=== 简柚博客 · ngrok 隧道（开发形态）===`n" -ForegroundColor Cyan

# 1) 环境检查
if (-not (Test-NgrokReady)) { exit 1 }

# 2) 端口检查
if (-not (Test-PortListening -Port $Port)) {
    if ($StartDev) {
        Write-Host "[..] 端口 $Port 未监听，尝试启动 Vite dev server" -ForegroundColor Yellow
        $webDir = Join-Path (Split-Path $PSScriptRoot -Parent) "blog-web"
        if (-not (Test-Path $webDir)) {
            Write-Host "[X] 未找到 blog-web 目录：$webDir" -ForegroundColor Red
            exit 1
        }
        Start-Process -FilePath "npm" -ArgumentList "run", "dev" -WorkingDirectory $webDir -WindowStyle Minimized
        if (-not (Wait-PortListening -Port $Port -TimeoutSec 90)) {
            Write-Host "[X] 等待 90 秒端口仍未监听，请手动执行：cd blog-web; npm run dev" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "[X] 端口 $Port 未监听" -ForegroundColor Red
        Write-Host "    先启动前端：cd blog-web; npm run dev" -ForegroundColor Yellow
        Write-Host "    或加参数自动启动：.\scripts\tunnel-dev.ps1 -StartDev" -ForegroundColor Yellow
        exit 1
    }
}
Write-Host "[OK] 端口 $Port 已监听" -ForegroundColor Green

# 3) 起隧道
#    走代理会被 ngrok 免费版拒绝（ERR_NGROK_9009），故先清掉代理环境变量
Clear-NgrokProxyEnv | Out-Null

Write-Host "`n[..] 正在启动 ngrok http $Port" -ForegroundColor Yellow
Write-Host "     按 Ctrl+C 结束隧道`n" -ForegroundColor DarkGray

# ngrok 自带 Web 控制台 http://127.0.0.1:4040
Start-Process "http://127.0.0.1:4040" -ErrorAction SilentlyContinue

# 捕获输出，便于出错时把 ERR_NGROK_xxx 翻译成人话
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
        Write-Host "`n=== 隧道建立失败，按错误码对照处理 ===`n" -ForegroundColor Red
        if ($text -match 'ERR_NGROK_9009|http/s proxy is a Pay-as-you-go') {
            Write-Host "[ERR_NGROK_9009] ngrok 免费版不允许走 HTTP 代理" -ForegroundColor Red
            Write-Host "  原因：系统存在 http_proxy / https_proxy 环境变量（常见于 Clash、v2ray 等代理工具）" -ForegroundColor Yellow
            Write-Host "  本脚本已尝试自动清除，若仍报此错，请手动检查：" -ForegroundColor Yellow
            Write-Host "    Get-ChildItem Env: | Where-Object Name -match 'proxy'" -ForegroundColor DarkGray
            Write-Host "  确认 ngrok 配置里没有 proxy_url：" -ForegroundColor Yellow
            Write-Host "    ngrok config edit" -ForegroundColor DarkGray
        }
        elseif ($text -match 'ERR_NGROK_105|authentication failed') {
            Write-Host "[ERR_NGROK_105] authtoken 无效或已过期" -ForegroundColor Red
            Write-Host "  重新获取：https://dashboard.ngrok.com/get-started/your-authtoken" -ForegroundColor Yellow
            Write-Host "  重新写入：ngrok config add-authtoken <新token>" -ForegroundColor Yellow
        }
        elseif ($text -match 'ERR_NGROK_108|account is limited|simultaneous') {
            Write-Host "[ERR_NGROK_108] 免费账号同时只能有 1 条隧道在线" -ForegroundColor Red
            Write-Host "  关掉其它 ngrok 窗口，或访问 http://127.0.0.1:4040 检查现有会话" -ForegroundColor Yellow
        }
        elseif ($text -match 'ERR_NGROK_3200|is offline') {
            Write-Host "[ERR_NGROK_3200] 隧道已离线（账号被暂停或 token 被吊销）" -ForegroundColor Red
            Write-Host "  登录控制台确认账号状态：https://dashboard.ngrok.com/status" -ForegroundColor Yellow
        }
        elseif ($text -match 'ERR_NGROK_4018|ERR_NGROK_4019|credit') {
            Write-Host "[ERR_NGROK_4018/4019] 额度不足或需绑定支付方式" -ForegroundColor Red
            Write-Host "  查看用量：https://dashboard.ngrok.com/billing" -ForegroundColor Yellow
        }
        elseif ($text -match 'ERR_NGROK_1\b|connect.*failed|dial tcp') {
            Write-Host "[网络错误] 连不上 ngrok 服务端" -ForegroundColor Red
            Write-Host "  检查网络连通性；可尝试更换区域或使用代理出口" -ForegroundColor Yellow
        }
        else {
            Write-Host "  未识别的错误，原始输出如下：" -ForegroundColor Yellow
        }

        # 打印含 ERR_NGROK 的原始行
        if ($text) {
            $lines = ($text -split "`r?`n") | Where-Object { $_ -match 'ERR_NGROK|error|failed' } | Select-Object -First 5
            foreach ($l in $lines) { Write-Host "  | $l" -ForegroundColor DarkGray }
        }
        Write-Host ""
    }

    Remove-Item $log -Force -ErrorAction SilentlyContinue
}
