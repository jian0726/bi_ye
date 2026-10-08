#!/usr/bin/env bash
# ============================================================
# 简柚个人博客 —— 一键部署脚本（Git Bash / Linux / macOS）
# 用法（在仓库根目录执行）：
#   bash scripts/deploy.sh            # 启动（复用已有镜像）
#   bash scripts/deploy.sh --rebuild  # 强制重建镜像
#   bash scripts/deploy.sh --down     # 停服
#
# 关键约定（勿改）：
#   1) 宿主机 3306/6379 已被本机服务占用，故映射 3307/6380；
#   2) 8080 被其他项目 Jenkins 占用，故后端对外映射 8081；
#   3) 端口覆盖必须【每条 compose 命令都写全】—— --force-recreate 不继承环境变量。
# ============================================================
set -e

cd "$(dirname "$0")/.."

# ---- 端口配置 ----
export BACKEND_PORT=8081
export FRONTEND_PORT=5174

echo "=== 简柚博客一键部署 ==="
echo "后端: http://localhost:${BACKEND_PORT}"
echo "前端: http://localhost:${FRONTEND_PORT}"
echo "MinIO 控制台: http://localhost:9001 (minioadmin / minioadmin2026)"
echo ""

if [ "$1" = "--down" ]; then
  echo ">>> 停止并移除容器（保留数据卷）..."
  docker compose down
  echo "已停服。"
  exit 0
fi

if [ "$1" = "--rebuild" ]; then
  echo ">>> 重新构建镜像（容器内 Maven/npm 编译，耗时较长）..."
  docker compose build
fi

echo ">>> 启动全部服务..."
docker compose up -d

echo ""
echo ">>> 等待后端就绪..."
ok=0
for i in $(seq 1 30); do
  sleep 2
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${BACKEND_PORT}/api/portal/site/stats" || true)
  if [ "$code" = "200" ]; then ok=1; break; fi
  echo "  等待中... ($i/30)"
done

echo ""
if [ "$ok" = "1" ]; then
  echo "=== 部署完成，后端已就绪 ==="
else
  echo "=== 后端未在预期时间内就绪，请查看日志：docker logs jianyou-backend ==="
fi

echo ""
echo "--- 容器状态 ---"
docker compose ps
