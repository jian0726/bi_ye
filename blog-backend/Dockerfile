# ---------- 构建阶段：容器内 Maven 编译（宿主机无需装 JDK/Maven） ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 阿里云镜像加速
COPY docker/settings.xml /root/.m2/settings.xml

# 先拷 pom 拉依赖，利用 Docker 层缓存
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# 再拷源码编译打包
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- 运行阶段：仅 JRE ----------
FROM eclipse-temurin:21-jre

WORKDIR /app
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# 以非 root 运行：创建专用用户，避免容器逃逸时拿到 root
RUN groupadd -r app && useradd -r -g app -d /app -s /sbin/nologin app \
    && mkdir -p /app/logs && chown -R app:app /app

COPY --from=build --chown=app:app /app/target/blog-backend.jar app.jar

USER app

EXPOSE 8080

# 健康检查：走业务侧的站点统计接口，能同时验证 Web 层与数据库连通
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:8080/api/portal/site/stats || exit 1

# exec 形式，确保 java 是 PID 1，能正确接收 SIGTERM 优雅停机
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
