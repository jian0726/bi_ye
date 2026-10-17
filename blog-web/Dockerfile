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

# 以非 root 运行：nginx:alpine 镜像内置 nginx 用户（uid 101）
# 需要把 nginx 的临时目录与 pid/日志路径改为可写，否则非 root 起不来
RUN sed -i 's#^user  nginx;##' /etc/nginx/nginx.conf \
    && sed -i 's#^\tpid        /var/run/nginx.pid;#\tpid        /tmp/nginx.pid;#' /etc/nginx/nginx.conf \
    && sed -i 's#^\terror_log  /var/log/nginx/error.log notice;#\terror_log  /dev/stderr notice;#' /etc/nginx/nginx.conf \
    && sed -i 's#^\taccess_log  /var/log/nginx/access.log  main;#\taccess_log  /dev/stdout main;#' /etc/nginx/nginx.conf \
    && sed -i 's#^http {#http {\n    client_body_temp_path /tmp/client_temp;\n    proxy_temp_path       /tmp/proxy_temp;\n    fastcgi_temp_path     /tmp/fastcgi_temp;\n    uwsgi_temp_path       /tmp/uwsgi_temp;\n    scgi_temp_path        /tmp/scgi_temp;#' /etc/nginx/nginx.conf \
    && chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx \
    && touch /tmp/nginx.pid && chown nginx:nginx /tmp/nginx.pid

USER nginx

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q --spider http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
