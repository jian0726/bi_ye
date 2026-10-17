# ---------- 构建阶段 ----------
FROM node:22-alpine AS build
WORKDIR /app

# 先只拷依赖清单，利用层缓存：源码变动不会触发重新安装
COPY package.json package-lock.json ./
# npm ci 严格按 lock 文件安装，保证构建可复现；缺少 lock 文件时直接失败而不是静默漂移
RUN npm ci --no-audit --no-fund

COPY . .
# build 脚本内含 vue-tsc 类型检查，类型不过则构建失败
RUN npm run build

# ---------- 运行阶段：nginx 托管 + API 反代 ----------
FROM nginx:alpine

# 时区，与后端保持一致
ENV TZ=Asia/Shanghai

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

# 安全响应头 snippet：每个 location 各自 include 一次
# （nginx 的 add_header 不跨层级继承：location 里写了任意一条，server 级的全部失效）
RUN mkdir -p /etc/nginx/snippets && cat > /etc/nginx/snippets/security-headers.conf <<'EOF'
# 禁止浏览器猜测 MIME（防止上传的 html 被当作页面执行）
add_header X-Content-Type-Options "nosniff" always;
# 禁止被第三方页面用 iframe 嵌套，防点击劫持
add_header X-Frame-Options "SAMEORIGIN" always;
# 来源信息只带域名，跨站跳转不泄漏完整路径
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
# 关闭不需要的浏览器能力
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;
# CSP：默认只允许同源，图片/媒体额外放行 data: 与 blob:（相册与音频播放需要）
# style-src 需 'unsafe-inline'：Vue 运行时与 Element Plus 会注入内联样式
add_header Content-Security-Policy "default-src 'self'; img-src 'self' data: blob:; media-src 'self' data: blob:; style-src 'self' 'unsafe-inline'; script-src 'self'; font-src 'self' data:; connect-src 'self'; frame-ancestors 'self'" always;
EOF

# 以非 root 运行：nginx:alpine 镜像内置 nginx 用户（uid 101）
# 需要把 nginx 的 pid / 日志 / 临时目录全部改到可写位置，否则非 root 直接起不来
# ⚠ 不要用"匹配行首缩进"的 sed：不同镜像版本缩进空格数不同（\t / 4 空格 / 2 空格），
#   实测 nginx:alpine 1.31 用的是 2 空格，导致 pid 行未被替换、容器启动即 permission denied。
#   这里统一用 sed -E 按行首任意空白匹配，幂等且不依赖缩进。
RUN sed -i -E 's#^[[:space:]]*user[[:space:]]+nginx;##' /etc/nginx/nginx.conf \
    && sed -i -E 's#^[[:space:]]*pid[[:space:]]+[^;]+;#pid /tmp/nginx.pid;#' /etc/nginx/nginx.conf \
    && sed -i -E 's#^[[:space:]]*error_log[[:space:]]+[^;]+;[[:space:]]*$#error_log /dev/stderr notice;#' /etc/nginx/nginx.conf \
    && sed -i -E 's#^[[:space:]]*access_log[[:space:]]+[^;]+;[[:space:]]*$#access_log /dev/stdout main;#' /etc/nginx/nginx.conf \
    && sed -i -E 's#^http[[:space:]]*\{#http {\n    client_body_temp_path /tmp/client_temp;\n    proxy_temp_path       /tmp/proxy_temp;\n    fastcgi_temp_path     /tmp/fastcgi_temp;\n    uwsgi_temp_path       /tmp/uwsgi_temp;\n    scgi_temp_path        /tmp/scgi_temp;#' /etc/nginx/nginx.conf \
    && chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx \
    && touch /tmp/nginx.pid && chown nginx:nginx /tmp/nginx.pid

# 构建期自检：替换没生效就当场失败，而不是等到运行时 Permission denied
RUN grep -qE '^[[:space:]]*pid[[:space:]]+/tmp/nginx.pid;' /etc/nginx/nginx.conf \
    && ! grep -qE '^[[:space:]]*user[[:space:]]+nginx;' /etc/nginx/nginx.conf \
    && grep -q '/tmp/client_temp;' /etc/nginx/nginx.conf \
    && grep -q 'Content-Security-Policy' /etc/nginx/snippets/security-headers.conf \
    && echo "nginx non-root config OK"

USER nginx

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q --spider http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
