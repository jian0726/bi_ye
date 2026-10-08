#!/usr/bin/env bash
# ============================================================
# 双 Token 会话机制 —— 冒烟验证脚本
# 配合 2026-10-07 实现的 refresh 轮换 / 撤销 / token 类型校验
#
# 用法：
#   1) 改下面 BASE（后端地址）与 ACCOUNT / PASSWORD（一个可登录的账号）
#   2) bash scripts/smoke-double-token.sh
#
# 覆盖 7 个断言（对应测试用例 TC-N 的 N01~N07）：
#   N01 登录返回 token + refreshToken
#   N02 refresh 换发新一对
#   N03 旧 refresh 复用被拒（轮换制核心）
#   N04 新 refresh 可继续换发
#   N05 refresh 冒充 access 访问受保护接口被拒
#   N06 access 冒充 refresh 被拒
#   N07 logout 后 refresh 失效
# ============================================================
set -u

BASE="${BASE:-http://localhost:8081/api}"
ACCOUNT="${ACCOUNT:-13800000001}"
PASSWORD="${PASSWORD:-}"

if [ -z "$PASSWORD" ]; then
  echo "请先设置账号密码：PASSWORD=xxx bash scripts/smoke-double-token.sh"
  exit 1
fi

PASS=0
FAIL=0
ok()   { echo "  ✓ $1"; PASS=$((PASS+1)); }
bad()  { echo "  ✗ $1"; FAIL=$((FAIL+1)); }

jqv() { python -c "import sys,json;d=json.load(sys.stdin);print(eval('d'+'$1'))" 2>/dev/null; }

echo "=============================================="
echo " 双 Token 冒烟验证  BASE=$BASE"
echo "=============================================="

# ---------- N01 登录拿双 token ----------
echo
echo "[N01] 登录返回双 Token"
LOGIN=$(curl -s -m 10 -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"account\":\"$ACCOUNT\",\"password\":\"$PASSWORD\"}")
CODE=$(echo "$LOGIN" | jqv "['code']")
AT=$(echo "$LOGIN"  | jqv "['data']['token']")
RT=$(echo "$LOGIN"  | jqv "['data']['refreshToken']")
if [ "$CODE" = "200" ] && [ -n "$AT" ] && [ "$AT" != "None" ] && [ -n "$RT" ] && [ "$RT" != "None" ]; then
  ok "code=200，access / refreshToken 均已返回"
else
  bad "登录失败：$LOGIN"
  echo; echo "登录不通，后续断言无法进行。"; exit 1
fi

# ---------- N02 refresh 换发新一对 ----------
echo
echo "[N02] 用 refresh 换发新一对"
R1=$(curl -s -m 10 -X POST "$BASE/auth/refresh" \
  -H "Content-Type: application/json" -d "{\"refreshToken\":\"$RT\"}")
C1=$(echo "$R1" | jqv "['code']")
AT1=$(echo "$R1" | jqv "['data']['token']")
RT1=$(echo "$R1" | jqv "['data']['refreshToken']")
if [ "$C1" = "200" ] && [ -n "$AT1" ] && [ "$AT1" != "None" ]; then
  ok "换发成功，拿到新的 access"
else
  bad "换发失败：$R1"
fi

# ---------- N03 旧 refresh 复用被拒 ----------
echo
echo "[N03] 旧 refresh 复用应被拒（轮换制）"
R2=$(curl -s -m 10 -X POST "$BASE/auth/refresh" \
  -H "Content-Type: application/json" -d "{\"refreshToken\":\"$RT\"}")
C2=$(echo "$R2" | jqv "['code']")
if [ "$C2" = "401" ]; then
  ok "旧 refresh 被拒（code=401），轮换生效"
else
  bad "旧 refresh 竟然还能用：$R2"
fi

# ---------- N04 新 refresh 可继续换发 ----------
echo
echo "[N04] 新 refresh 可继续换发"
R3=$(curl -s -m 10 -X POST "$BASE/auth/refresh" \
  -H "Content-Type: application/json" -d "{\"refreshToken\":\"$RT1\"}")
C3=$(echo "$R3" | jqv "['code']")
RT2=$(echo "$R3" | jqv "['data']['refreshToken']")
if [ "$C3" = "200" ]; then
  ok "连续轮换正常（新 refresh 可用）"
else
  bad "新 refresh 不可用：$R3"
fi

# ---------- N05 refresh 冒充 access ----------
echo
echo "[N05] refresh 当请求凭证访问 /auth/me 应被拒"
ME=$(curl -s -m 10 "$BASE/auth/me" -H "Authorization: Bearer $RT2")
CM=$(echo "$ME" | jqv "['code']")
if [ "$CM" = "401" ]; then
  ok "refresh 冒充 access 被拒（code=401）"
else
  bad "refresh 竟能当 access 用：$ME"
fi

# ---------- N06 access 冒充 refresh ----------
echo
echo "[N06] access 提交给 /auth/refresh 应被拒"
R4=$(curl -s -m 10 -X POST "$BASE/auth/refresh" \
  -H "Content-Type: application/json" -d "{\"refreshToken\":\"$AT\"}")
C4=$(echo "$R4" | jqv "['code']")
if [ "$C4" = "401" ] || [ "$C4" = "400" ]; then
  ok "access 冒充 refresh 被拒（code=$C4）"
else
  bad "access 竟能当 refresh 用：$R4"
fi

# ---------- N07 logout 后 refresh 失效 ----------
echo
echo "[N07] logout 后 refresh 立即失效"
LO=$(curl -s -m 10 -X POST "$BASE/auth/logout" \
  -H "Content-Type: application/json" -d "{\"refreshToken\":\"$RT2\"}")
RO=$(curl -s -m 10 -X POST "$BASE/auth/refresh" \
  -H "Content-Type: application/json" -d "{\"refreshToken\":\"$RT2\"}")
CO=$(echo "$RO" | jqv "['code']")
if [ "$CO" = "401" ]; then
  ok "logout 后该 refresh 被拒（code=401），撤销生效"
else
  bad "logout 后 refresh 仍可用：$RO"
fi

echo
echo "=============================================="
echo " 结果：通过 $PASS / 失败 $FAIL"
echo "=============================================="
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
