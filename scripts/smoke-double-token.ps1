# ============================================================
# 双 Token 会话机制 —— 冒烟验证（PowerShell 版）
# 对应测试用例 TC-N 的 N01~N07
#
# 用法：
#   $env:BASE="http://localhost:8081/api"
#   $env:PASSWORD="<站长密码>"
#   powershell -ExecutionPolicy Bypass -File scripts/smoke-double-token.ps1
# ============================================================

$ErrorActionPreference = "Stop"

$BASE     = if ($env:BASE)     { $env:BASE }     else { "http://localhost:8081/api" }
$ACCOUNT  = if ($env:ACCOUNT)  { $env:ACCOUNT }  else { "13800000001" }
$PASSWORD = $env:PASSWORD

if (-not $PASSWORD) {
    Write-Host "请先设置密码：`$env:PASSWORD='<站长密码>'" -ForegroundColor Yellow
    exit 1
}

$pass = 0; $fail = 0
function OK($m)   { Write-Host "  [OK] $m" -ForegroundColor Green;  $script:pass++ }
function BAD($m)  { Write-Host "  [X]  $m" -ForegroundColor Red;    $script:fail++ }

function Invoke-Json($Method, $Url, $Body) {
    $p = @{ Method = $Method; Uri = $Url; ContentType = "application/json; charset=utf-8" }
    if ($Body) { $p.Body = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Compress)) }
    try {
        return Invoke-RestMethod @p -TimeoutSec 10
    } catch {
        $r = $_.Exception.Response
        if ($r) {
            $sr = New-Object System.IO.StreamReader($r.GetResponseStream())
            $txt = $sr.ReadToEnd()
            try { return ($txt | ConvertFrom-Json) } catch { return @{ code = -1; message = $txt } }
        }
        return @{ code = -1; message = $_.Exception.Message }
    }
}

Write-Host "=============================================="
Write-Host " 双 Token 冒烟验证  BASE=$BASE"
Write-Host "=============================================="

# ---------- N01 ----------
Write-Host "`n[N01] 登录返回双 Token"
$login = Invoke-Json POST "$BASE/auth/login" @{ account = $ACCOUNT; password = $PASSWORD }
$AT = $login.data.token
$RT = $login.data.refreshToken
if ($login.code -eq 200 -and $AT -and $RT) {
    OK "code=200，access / refreshToken 均已返回"
} else {
    BAD "登录失败：$($login | ConvertTo-Json -Compress)"
    Write-Host "`n登录不通，后续断言无法进行。"
    exit 1
}

# ---------- N02 ----------
Write-Host "`n[N02] 用 refresh 换发新一对"
$r1 = Invoke-Json POST "$BASE/auth/refresh" @{ refreshToken = $RT }
$AT1 = $r1.data.token; $RT1 = $r1.data.refreshToken
if ($r1.code -eq 200 -and $AT1) { OK "换发成功，拿到新的 access" }
else { BAD "换发失败：$($r1 | ConvertTo-Json -Compress)" }

# ---------- N03 ----------
Write-Host "`n[N03] 旧 refresh 复用应被拒（轮换制）"
$r2 = Invoke-Json POST "$BASE/auth/refresh" @{ refreshToken = $RT }
if ($r2.code -eq 401) { OK "旧 refresh 被拒（code=401），轮换生效" }
else { BAD "旧 refresh 竟然还能用：$($r2 | ConvertTo-Json -Compress)" }

# ---------- N04 ----------
Write-Host "`n[N04] 新 refresh 可继续换发"
$r3 = Invoke-Json POST "$BASE/auth/refresh" @{ refreshToken = $RT1 }
$RT2 = $r3.data.refreshToken
if ($r3.code -eq 200) { OK "连续轮换正常（新 refresh 可用）" }
else { BAD "新 refresh 不可用：$($r3 | ConvertTo-Json -Compress)" }

# ---------- N05 ----------
Write-Host "`n[N05] refresh 当请求凭证访问 /auth/me 应被拒"
$me = Invoke-Json GET "$BASE/auth/me" $null
try {
    $me = Invoke-RestMethod -Method GET -Uri "$BASE/auth/me" `
        -Headers @{ Authorization = "Bearer $RT2" } -TimeoutSec 10
} catch {
    $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $me = $sr.ReadToEnd() | ConvertFrom-Json
}
if ($me.code -eq 401) { OK "refresh 冒充 access 被拒（code=401）" }
else { BAD "refresh 竟能当 access 用：$($me | ConvertTo-Json -Compress)" }

# ---------- N06 ----------
Write-Host "`n[N06] access 提交给 /auth/refresh 应被拒"
$r4 = Invoke-Json POST "$BASE/auth/refresh" @{ refreshToken = $AT }
if ($r4.code -eq 401 -or $r4.code -eq 400) { OK "access 冒充 refresh 被拒（code=$($r4.code)）" }
else { BAD "access 竟能当 refresh 用：$($r4 | ConvertTo-Json -Compress)" }

# ---------- N07 ----------
Write-Host "`n[N07] logout 后 refresh 立即失效"
$null = Invoke-Json POST "$BASE/auth/logout" @{ refreshToken = $RT2 }
$ro = Invoke-Json POST "$BASE/auth/refresh" @{ refreshToken = $RT2 }
if ($ro.code -eq 401) { OK "logout 后该 refresh 被拒（code=401），撤销生效" }
else { BAD "logout 后 refresh 仍可用：$($ro | ConvertTo-Json -Compress)" }

Write-Host "`n=============================================="
Write-Host " 结果：通过 $pass / 失败 $fail"
Write-Host "=============================================="
if ($fail -eq 0) { exit 0 } else { exit 1 }
