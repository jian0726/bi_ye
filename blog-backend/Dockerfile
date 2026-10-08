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
ENV TZ=Asia/Shanghai
COPY --from=build /app/target/blog-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
