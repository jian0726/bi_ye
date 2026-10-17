# 检查 ngrok 是否已安装并配置 authtoken；未配置则给出明确指引
# 用法：. .\scripts\_ngrok-precheck.ps1
#
# 修正说明（2026-10-17 第二次修正）：
#   1) 旧版用 Test-Path 探测固定配置路径 → 在 Store 版 ngrok / 路径虚拟化环境下误判
#   2) 改用 `ngrok config get authtoken` → 该子命令在 v3 中并不存在，会打印帮助文本，
#      导致把帮助文本误当成 token（曾出现 "authtoken 已配置：NAME****nfig" 的假象）
#   最终方案：只用 `ngrok config check` 判配置有效性；token 是否可用交由实际建隧道验证。

function Test-NgrokReady {
    $ngrok = Get-Command ngrok -ErrorAction SilentlyContinue
    if (-not $ngrok) {
        Write-Host "[X] 未安装 ngrok" -ForegroundColor Red
        Write-Host "    安装：winget install ngrok.ngrok" -ForegroundColor Yellow
        Write-Host "    或下载：https://ngrok.com/download" -ForegroundColor Yellow
        return $false
    }
    Write-Host "[OK] ngrok 已安装：$($ngrok.Source)" -ForegroundColor Green

    # 只做一件事：配置能否被 ngrok 识别为有效（存在、语法正确、含 authtoken 字段）
    $check = (& ngrok config check 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $check -match 'error|no such|not found') {
        Write-Host "[X] ngrok 配置无效或不存在" -ForegroundColor Red
        if ($check) { Write-Host "    $check" -ForegroundColor DarkGray }
        Write-Host "    执行：ngrok config add-authtoken <你的token>" -ForegroundColor Yellow
        Write-Host "    token 获取：https://dashboard.ngrok.com/get-started/your-authtoken" -ForegroundColor Yellow
        return $false
    }
    Write-Host "[OK] 配置有效：$check" -ForegroundColor Green
    Write-Host "     （token 是否可用，由下方实际建立隧道时验证）" -ForegroundColor DarkGray
    return $true
}



function Test-PortListening {
    param([int]$Port)
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return [bool]$conn
}

function Wait-PortListening {
    param([int]$Port, [int]$TimeoutSec = 60)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortListening -Port $Port) { return $true }
        Start-Sleep -Milliseconds 800
    }
    return $false
}

# 清除 ngrok 不支持的 HTTP 代理环境变量（仅作用于当前进程，不改系统设置）
# 背景：ngrok 免费版禁止 agent 走 http/s 代理，检测到 http_proxy/https_proxy
#       会直接以 ERR_NGROK_9009 拒绝连接（提示需升级 Pay-as-you-go）。
#       国内常见的 Clash/v2ray 等工具会把这类变量注入用户环境变量。
function Clear-NgrokProxyEnv {
    $names = @("http_proxy", "https_proxy", "HTTP_PROXY", "HTTPS_PROXY", "all_proxy", "ALL_PROXY")
    $cleared = @()
    foreach ($n in $names) {
        $v = [Environment]::GetEnvironmentVariable($n, "Process")
        if ($v) {
            [Environment]::SetEnvironmentVariable($n, $null, "Process")
            $cleared += "$n=$v"
            # 同步清掉 PowerShell 的 $env: 视图
            Remove-Item -Path "Env:$n" -ErrorAction SilentlyContinue
        }
    }
    if ($cleared.Count -gt 0) {
        Write-Host "[!] 已临时清除代理变量（ngrok 免费版不支持代理，否则报 ERR_NGROK_9009）" -ForegroundColor Yellow
        foreach ($c in $cleared) { Write-Host "    $c" -ForegroundColor DarkGray }
        Write-Host "    仅影响本窗口，系统环境变量未改动" -ForegroundColor DarkGray
    }
    return $cleared.Count
}

