# 黑马商城 (hmall) Docker 部署指南

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 架构说明](#2-架构说明)
- [3. 准备工作](#3-准备工作)
- [4. 已做修改说明](#4-已做修改说明)
- [5. 操作步骤](#5-操作步骤)
- [6. 访问地址](#6-访问地址)
- [7. 常用命令](#7-常用命令)
- [8. 开发工作流](#8-开发工作流)
- [9. 故障排查](#9-故障排查)

---

## 1. 项目概览

| 项目 | 说明 |
|------|------|
| 名称 | 黑马商城 (hmall) |
| 类型 | Spring Boot 微服务电商项目 |
| 构建工具 | Maven |
| Java 版本 | 11 |
| Spring Boot | 2.7.12 |
| Spring Cloud | 2021.0.3 |
| 数据库 | MySQL 8.0（8 张表） |
| 缓存 | Redis |
| 前端 | Nginx 托管静态页面（portal 用户端 + admin 管理端） |

### 项目结构

```
hmall/
├── hm-common/              # 公共模块：工具类、异常处理、通用配置
├── hm-service/             # 主服务模块：Spring Boot 入口、Controller、Service、Mapper
│   ├── src/main/resources/
│   │   ├── application.yaml        # 主配置（端口 8080，dev 环境）
│   │   ├── application-dev.yaml    # dev 环境：数据库/Redis 连接地址
│   │   └── application-local.yaml  # 本地环境配置
│   └── Dockerfile                  # 后端 Dockerfile（多阶段构建）
├── mysql/
│   ├── conf/hm.cnf         # MySQL 自定义配置（utf8mb4）
│   └── init/hmall.sql      # 数据库初始化脚本（建库 + 建表 + 种子数据）
├── nginx/
│   ├── nginx.conf          # Nginx 配置（18080 portal / 18081 admin，API 代理到后端）
│   └── html/               # 前端静态文件
│       ├── hmall-portal/   # 用户端页面（首页、登录、购物车、支付等）
│       └── hmall-admin/    # 管理端页面
├── images/                 # 预置 Docker 镜像 tar 包（离线部署用）
│   ├── jdk.tar             # openjdk:11.0-jre-buster
│   ├── mysql.tar           # mysql:latest
│   └── nginx.tar           # nginx:latest
├── docker-compose.yml      # Docker 编排文件（一键启动所有服务）
├── .dockerignore           # Docker 构建排除文件
└── pom.xml                 # Maven 父 POM
```

---

## 2. 架构说明

```
浏览器
  │
  ├─ http://localhost:18080  →  Nginx (hmall-nginx)
  │                               ├─ /             → 用户端静态页面
  │                               └─ /api/*        → 反代到 hmall-service:8080
  │
  └─ http://localhost:18081  →  Nginx (hmall-nginx)
                                  ├─ /             → 管理端静态页面
                                  └─ /api/*        → 反代到 hmall-service:8080

                                          hmall-service:8080 (Spring Boot)
                                               │
                                    ┌──────────┼──────────┐
                                    ↓                     ↓
                              hmall-mysql:3306      hmall-redis:6379
```

**服务清单：**

| 容器名 | 镜像 | 端口 | 说明 |
|--------|------|------|------|
| `hmall-mysql` | `mysql:latest` | 3307 | MySQL 数据库，首次启动自动执行 `hmall.sql` |
| `hmall-redis` | `redis:7-alpine` | 6380 | Redis 缓存 |
| `hmall-service` | `hmall-service:latest`（本地构建） | 8081 | Spring Boot 后端服务 |
| `hmall-nginx` | `nginx:latest` | 18080, 18081 | 前端代理 |

---

## 3. 准备工作

### 3.1 安装 Docker

- **macOS**: 安装 [Docker Desktop for Mac](https://docs.docker.com/desktop/setup/install/mac-install/)
- **Windows**: 安装 [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/)
- **Linux**: 安装 [Docker Engine](https://docs.docker.com/engine/install/) + [Docker Compose](https://docs.docker.com/compose/install/)

安装完成后验证：

```bash
docker --version       # 需要 20.10+
docker compose version # 需要 2.0+
```

### 3.2 调整 Docker 资源（推荐）

Docker Desktop → Settings → Resources：

- **Memory**: 至少 4GB（建议 8GB）
- **CPU**: 至少 2 核

---

## 4. 已做修改说明

为支持 Docker 部署，对项目做了以下修改：

### 4.1 新建文件

| 文件 | 说明 |
|------|------|
| `docker-compose.yml` | 编排 4 个服务（MySQL + Redis + 后端 + Nginx），配置网络、数据卷、健康检查 |
| `.dockerignore` | 排除 IDE 配置、本地构建产物、images/ 等，加速 Docker 构建 |

### 4.2 修改文件

| 文件 | 改动 | 原因 |
|------|------|------|
| `hm-service/Dockerfile` | 从简单 COPY 改为**多阶段构建**（Maven 编译 → JRE 运行） | 无需本地安装 Maven 和 JDK，Docker 内一站式构建 |
| `hm-service/src/main/resources/application-dev.yaml` | 新增 `spring.redis.host: redis` | 让 Spring Boot 在 Docker 网络中正确连接 Redis |
| `pom.xml` | Lombok 版本 `1.18.20` → `1.18.46` | 兼容 JDK 26（本地 IDE 编译不再报错） |
| `hm-common/.../JsonConfig_20231023_150602.java` | 重命名为 `JsonConfig.java` | 修复文件名与 public 类名不匹配的编译错误 |

### 4.3 关键配置对应关系

Spring Boot 的 `application.yaml` 使用了占位符 `${hm.db.host}` 和 `${hm.db.pw}`：

```yaml
# application.yaml
spring:
  datasource:
    url: jdbc:mysql://${hm.db.host}:3306/hmall?...
    password: ${hm.db.pw}
```

这些值由 **docker-compose.yml** 中的环境变量注入：

```yaml
# docker-compose.yml
hmall:
  environment:
    HM_DB_HOST: mysql          # → ${hm.db.host}
    HM_DB_PW: "123"            # → ${hm.db.pw}
    SPRING_REDIS_HOST: redis   # → spring.redis.host
```

> Spring Boot 会自动将 `HM_DB_HOST` 这样的环境变量映射为 `hm.db.host` 配置属性（用 `_` 替代 `.`，大写转小写）。

---

## 5. 操作步骤

### 5.1 克隆/进入项目目录

```bash
cd /Users/Admin/IdeaProjects/hmall
```

### 5.2 【可选】加载预置 Docker 镜像

如果网络不好或需要离线部署，可以先加载 `images/` 目录下的镜像 tar 包：

```bash
docker load -i images/jdk.tar      # openjdk:11.0-jre-buster
docker load -i images/mysql.tar    # mysql:latest
docker load -i images/nginx.tar    # nginx:latest
```

> 网络正常的话可以跳过这一步，`docker compose` 会自动从 Docker Hub 拉取所需镜像。

### 5.3 一键启动

```bash
docker compose up -d
```

首次运行时会自动完成以下步骤：

1. 拉取基础镜像（maven:3.8-openjdk-11、openjdk:11.0-jre-buster、mysql、redis、nginx）
2. 在容器内用 Maven 编译 Java 源码，打包成 `hm-service.jar`
3. 构建 `hmall-service:latest` 镜像
4. 启动 MySQL 容器，自动执行 `mysql/init/hmall.sql` 初始化数据库
5. 启动 Redis 容器
6. 等待 MySQL 和 Redis 健康检查通过后，启动 Spring Boot 应用
7. 启动 Nginx 容器

**预计耗时**：首次构建 5~10 分钟（主要花在 Maven 下载依赖和编译），后续启动仅需几十秒。

### 5.4 验证服务状态

```bash
# 查看所有容器运行状态
docker compose ps

# 期望输出：4 个容器状态都是 Up (healthy) 或 Up
```

```bash
# 查看后端启动日志（Ctrl+C 退出）
docker compose logs -f hmall

# 看到以下日志表示启动成功：
# Started HMallApplication in X.XXX seconds
```

```bash
# 验证 MySQL 数据库初始化是否成功
docker exec -it hmall-mysql mysql -uroot -p123 -e "USE hmall; SHOW TABLES;"

# 期望输出 8 张表：
# address, cart, item, order, order_detail, order_logistics, pay_order, user
```

---

## 6. 访问地址

| 页面 | URL | 说明 |
|------|-----|------|
| 🛍️ 用户端商城 | http://localhost:18080 | 商品浏览、购物车、下单、支付 |
| ⚙️ 管理后台 | http://localhost:18081/users.html | 商品管理、订单管理 |
| 📖 API 文档 (Knife4j) | http://localhost:8081/doc.html | Swagger 接口文档 |

---

## 7. 常用命令

### 服务管理

```bash
# 启动所有服务
docker compose up -d

# 查看运行状态
docker compose ps

# 查看所有服务日志
docker compose logs -f

# 查看单个服务日志
docker compose logs -f hmall
docker compose logs -f mysql

# 停止所有服务（保留数据）
docker compose down

# 停止并删除所有数据（数据库重置）
docker compose down -v

# 重启某个服务
docker compose restart hmall
docker compose restart nginx
```

### 重新构建

```bash
# 修改了 Java 代码后重新构建后端
docker compose up -d --build hmall

# 强制完全重建（不使用缓存）
docker compose build --no-cache hmall
docker compose up -d
```

### 数据库操作

```bash
# 进入 MySQL 命令行
docker exec -it hmall-mysql mysql -uroot -p123

# 手动重新导入 SQL
docker exec -i hmall-mysql mysql -uroot -p123 hmall < mysql/init/hmall.sql
```

### 进入容器调试

```bash
# 进入后端容器
docker exec -it hmall-service bash

# 进入 Redis
docker exec -it hmall-redis redis-cli
```

---

## 8. 开发工作流

### 修改 Java 代码

```bash
# 1. 在 IDE 中修改代码
# 2. 重新构建并重启后端
docker compose up -d --build hmall
# 3. 验证
docker compose logs -f hmall
```

### 修改前端页面

前端文件在 `nginx/html/` 下，通过 Volume 挂载到 Nginx 容器中，**修改后无需重启**，直接刷新浏览器即可生效。

```bash
# 修改用户端首页
vim nginx/html/hmall-portal/index.html
# 浏览器刷新 http://localhost:18080 即可看到变化
```

### 修改 Nginx 配置

```bash
vim nginx/nginx.conf
docker compose restart nginx
```

### 数据库重置

```bash
# 完全重置数据库（删除数据卷）
docker compose down -v
docker compose up -d
```

---

## 9. 故障排查

### 9.1 容器启动失败

```bash
# 查看具体错误
docker compose logs <服务名>

# 常见原因：
# - 端口被占用：修改 docker-compose.yml 中的端口映射
# - 内存不足：给 Docker Desktop 多分配内存
```

### 9.2 数据库初始化失败（表没有创建）

**原因**：MySQL 的 Data Volume 已存在（之前启动过），初始化脚本只在**数据目录首次创建时**执行一次。

```bash
# 解决方法：清除数据卷重新初始化
docker compose down -v
docker compose up -d
```

### 9.3 后端连不上 MySQL / Redis

```bash
# 检查网络是否正常
docker exec hmall-service ping mysql
docker exec hmall-service ping redis

# 检查 MySQL 是否就绪
docker exec hmall-mysql mysqladmin ping -uroot -p123

# 检查 Redis 是否就绪
docker exec hmall-redis redis-cli ping
```

### 9.4 编译报错（Lombok 相关）

本项目的 Lombok 已升级到 **1.18.46**，兼容 JDK 26。如果仍有问题：

```bash
# 确认 pom.xml 中的 Lombok 版本
grep "lombok.version" pom.xml
# 应该看到：<org.projectlombok.version>1.18.46</org.projectlombok.version>
```

### 9.5 端口被占用

```bash
# 查看端口占用情况
lsof -i :8080
lsof -i :3306
lsof -i :18080

# 修改 docker-compose.yml 中的端口映射（前面的宿主机端口）：
# "18080:18080" 改为 "18082:18080" 等
```

### 9.6 Docker Desktop 内存不足

症状：后端频繁重启，日志显示 `OutOfMemoryError` 或 MySQL 启动超时。

解决：Docker Desktop → Settings → Resources → Memory → 调至 8GB。