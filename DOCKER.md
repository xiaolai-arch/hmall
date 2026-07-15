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
| 类型 | Spring Cloud 微服务电商项目 |
| 构建工具 | Maven |
| Java 版本 | 11 |
| Spring Boot | 2.7.12 |
| Spring Cloud | 2021.0.3 |
| Spring Cloud Alibaba | 2021.0.4.0 |
| 注册中心 | Nacos 2.2.3 |
| 网关 | Spring Cloud Gateway |
| 数据库 | MySQL 8.0（8 张业务表） |
| 缓存 | Redis 7 |
| 前端 | Nginx 托管静态页面（portal 用户端 + admin 管理端） |

### 项目结构

```
hmall/
├── hm-common/              # 公共模块：工具类、异常处理、通用配置
├── hm-api/                 # API 模块：Feign 接口定义
├── hm-gateway/             # 网关模块：Spring Cloud Gateway 路由转发
├── hm-service/             # 主服务模块：通用业务
├── item-service/           # 商品服务：商品查询、搜索
├── cart-service/           # 购物车服务
├── user-service/           # 用户服务：登录、地址管理
├── trade-service/          # 交易服务：订单管理
├── pay-service/            # 支付服务
├── mysql/
│   ├── conf/hm.cnf         # MySQL 自定义配置（utf8mb4）
│   └── init/               # 数据库初始化脚本（按文件顺序自动执行）
│       ├── hmall.sql       # 主库 hmall
│       ├── 02-hm-item.sql  # 商品库 hm-item
│       ├── 03-hm-cart.sql  # 购物车库 hm-cart
│       ├── 04-hm-user.sql  # 用户库 hm-user
│       ├── 05-hm-trade.sql # 交易库 hm-trade
│       ├── 06-hm-pay.sql   # 支付库 hm-pay
│       └── 07-nacos.sql    # Nacos 配置库 nacos
├── nginx/
│   ├── nginx.conf          # Nginx 配置（18080 portal / 18081 admin，API 代理到网关）
│   └── html/               # 前端静态文件
│       ├── hmall-portal/   # 用户端页面（首页、登录、购物车、支付等）
│       └── hmall-admin/    # 管理端页面
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
  │                               └─ /api/*        → 反代到 hm-gateway:8080
  │
  └─ http://localhost:18081  →  Nginx (hmall-nginx)
                                    ├─ /             → 管理端静态页面
                                    └─ /api/*        → 反代到 hm-gateway:8080

                                    hm-gateway:8080 (Spring Cloud Gateway)
                                         │
                                    ┌────┼──────────────┐
    Nacos 注册中心                  │    │              │
    (服务发现 & 配置管理)            │    │              │
         │                         ↓    ↓              ↓
    nacos:8848              item-service   cart-service   user-service
         │                   :8082          :8083           :8084
         │
    hm-gateway  ──────────── trade-service  pay-service    hm-service
    :8080                     :8085          :8086           :8080

    ┌─────────────────────────────────────────────────────────┐
    │                    基础设施层                             │
    │  hmall-mysql:3306    hmall-redis:6379                   │
    │  (8 个业务库)        (缓存)                              │
    └─────────────────────────────────────────────────────────┘
```

**服务清单：**

| 容器名 | 镜像 | 端口 (宿主机:容器) | 说明 |
|--------|------|-------------------|------|
| `hmall-mysql` | `mysql:8.0` | 3307:3306 | MySQL 数据库，首次启动自动初始化所有库表 |
| `hmall-redis` | `redis:7-alpine` | 6380:6379 | Redis 缓存 |
| `hmall-nacos` | `nacos/nacos-server:v2.2.3` | 8848:8848, 9848:9848 | Nacos 注册中心 & 配置中心 |
| `hmall-gateway` | `hmall-gateway:latest`（本地构建） | 8080:8080 | Spring Cloud Gateway 网关 |
| `hmall-item-service` | `hmall-item-service:latest`（本地构建） | 8082:8082 | 商品服务 |
| `hmall-cart-service` | `hmall-cart-service:latest`（本地构建） | 8083:8083 | 购物车服务 |
| `hmall-user-service` | `hmall-user-service:latest`（本地构建） | 8084:8084 | 用户服务 |
| `hmall-trade-service` | `hmall-trade-service:latest`（本地构建） | 8085:8085 | 交易服务 |
| `hmall-pay-service` | `hmall-pay-service:latest`（本地构建） | 8086:8086 | 支付服务 |
| `hmall-service` | `hmall-service:latest`（本地构建） | 8081:8080 | 通用主服务 |
| `hmall-nginx` | `nginx:stable-alpine` | 18080:18080, 18081:18081 | 前端代理 |

**数据库清单：**

| 数据库名 | 所属服务 | 说明 |
|----------|----------|------|
| `hmall` | hm-service | 主库（用户地址等） |
| `hm-item` | item-service | 商品、搜索 |
| `hm-cart` | cart-service | 购物车 |
| `hm-user` | user-service | 用户 |
| `hm-trade` | trade-service | 订单、物流 |
| `hm-pay` | pay-service | 支付 |
| `nacos` | nacos | Nacos 配置持久化 |

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

- **Memory**: 至少 8GB（微服务架构需要更多内存）
- **CPU**: 至少 4 核

---

## 4. 已做修改说明

为支持 Docker 部署，对项目做了以下修改：

### 4.1 新建文件

| 文件 | 说明 |
|------|------|
| `docker-compose.yml` | 编排 11 个服务（MySQL + Redis + Nacos + Gateway + 6 微服务 + Nginx），配置网络、数据卷、健康检查 |
| `.dockerignore` | 排除 IDE 配置、本地构建产物等，加速 Docker 构建 |
| `hm-gateway/Dockerfile` | 网关模块多阶段构建 |
| `item-service/Dockerfile` | 商品服务多阶段构建 |
| `cart-service/Dockerfile` | 购物车服务多阶段构建 |
| `user-service/Dockerfile` | 用户服务多阶段构建 |
| `trade-service/Dockerfile` | 交易服务多阶段构建 |
| `pay-service/Dockerfile` | 支付服务多阶段构建 |

### 4.2 关键配置对应关系

**数据库连接**（以 item-service 为例）：

Spring Boot 的 `application.yaml` 使用了占位符 `${hm.db.host}`、`${hm.db.port}` 和 `${hm.db.pw}`：

```yaml
# application.yaml
spring:
  datasource:
    url: jdbc:mysql://${hm.db.host}:${hm.db.port:3306}/hm-item?...
    password: ${hm.db.pw}
```

这些值由 **docker-compose.yml** 中的环境变量注入：

```yaml
# docker-compose.yml
item-service:
  environment:
    HM_DB_HOST: mysql          # → ${hm.db.host}
    HM_DB_PORT: "3306"         # → ${hm.db.port}
    HM_DB_PW: "123"            # → ${hm.db.pw}
```

**Nacos 连接**：

```yaml
# application.yaml 中硬编码了 localhost:8848
spring:
  cloud:
    nacos:
      server-addr: localhost:8848
```

Docker 中通过环境变量覆盖：

```yaml
# docker-compose.yml
environment:
  SPRING_CLOUD_NACOS_SERVER_ADDR: nacos:8848   # 覆盖 localhost:8848
```

> Spring Boot 会自动将 `HM_DB_HOST` 映射为 `hm.db.host`，将 `SPRING_CLOUD_NACOS_SERVER_ADDR` 映射为 `spring.cloud.nacos.server-addr`。环境变量优先级高于 application.yaml。

---

## 5. 操作步骤

### 5.1 进入项目目录

```bash
cd hmall
```

### 5.2 一键启动

```bash
docker compose up -d
```

首次运行时会自动完成以下步骤：

1. 拉取基础镜像（mysql:8.0, redis:7-alpine, nacos/nacos-server:v2.2.3, nginx:stable-alpine, maven:3.8-eclipse-temurin-11, openjdk:11.0-jre-buster）
2. 启动 MySQL 容器，自动执行 `mysql/init/` 下所有 SQL 脚本初始化 7 个数据库
3. 启动 Redis 容器
4. 等待 MySQL 健康检查通过后，启动 Nacos 容器（Nacos 使用 MySQL 持久化配置）
5. 等待 Nacos 健康检查通过后，依次构建并启动各微服务：
   - 在容器内用 Maven 编译 Java 源码，打包成 jar
   - 构建服务镜像
   - 启动服务并自动注册到 Nacos
6. 启动 Spring Cloud Gateway 网关
7. 启动 Nginx 容器

**预计耗时**：首次构建 10~20 分钟（主要花在 Maven 下载依赖和各服务编译），后续启动仅需几十秒。

### 5.3 验证服务状态

```bash
# 查看所有容器运行状态
docker compose ps

# 期望输出：11 个容器状态都是 Up (healthy) 或 Up
```

```bash
# 查看某个服务启动日志（Ctrl+C 退出）
docker compose logs -f item-service
docker compose logs -f hm-gateway

# 看到以下日志表示启动成功：
# Started XxxApplication in X.XXX seconds
```

```bash
# 验证 Nacos 注册中心
# 浏览器访问 http://localhost:8848/nacos
# 默认用户名/密码：nacos/nacos
# 在"服务管理 → 服务列表"中应该能看到所有已注册的服务
```

```bash
# 验证 MySQL 数据库初始化是否成功
docker exec -it hmall-mysql mysql -uroot -p123 -e "SHOW DATABASES;"

# 期望看到 7 个数据库：
# hmall, hm-item, hm-cart, hm-user, hm-trade, hm-pay, nacos
```

---

## 6. 访问地址

| 页面 | URL | 说明 |
|------|-----|------|
| 🛍️ 用户端商城 | http://localhost:18080 | 商品浏览、购物车、下单、支付 |
| ⚙️ 管理后台 | http://localhost:18081/users.html | 商品管理、订单管理 |
| 🚪 API 网关 | http://localhost:8080 | Spring Cloud Gateway 统一入口 |
| 🧭 Nacos 控制台 | http://localhost:8848/nacos | 服务列表、配置管理（用户名/密码：nacos/nacos） |
| 📖 API 文档 (Knife4j) | http://localhost:8082/doc.html | 商品服务接口文档 |
| 📖 API 文档 | http://localhost:8083/doc.html | 购物车服务接口文档 |
| 📖 API 文档 | http://localhost:8084/doc.html | 用户服务接口文档 |
| 📖 API 文档 | http://localhost:8085/doc.html | 交易服务接口文档 |
| 📖 API 文档 | http://localhost:8086/doc.html | 支付服务接口文档 |

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
docker compose logs -f hm-gateway
docker compose logs -f item-service
docker compose logs -f nacos

# 停止所有服务（保留数据）
docker compose down

# 停止并删除所有数据（数据库重置）
docker compose down -v

# 重启某个服务
docker compose restart hm-gateway
docker compose restart item-service
docker compose restart nginx
```

### 重新构建

```bash
# 修改了某个模块的代码后重新构建
docker compose up -d --build item-service
docker compose up -d --build hm-gateway

# 强制完全重建（不使用缓存）
docker compose build --no-cache item-service
docker compose up -d

# 重建所有服务
docker compose up -d --build
```

### 数据库操作

```bash
# 进入 MySQL 命令行
docker exec -it hmall-mysql mysql -uroot -p123

# 手动重新导入某个数据库
docker exec -i hmall-mysql mysql -uroot -p123 hm-item < mysql/init/02-hm-item.sql
```

### 进入容器调试

```bash
# 进入某个微服务容器
docker exec -it hmall-gateway bash
docker exec -it hmall-item-service bash

# 进入 Redis
docker exec -it hmall-redis redis-cli

# 查看 Nacos 日志
docker logs -f hmall-nacos
```

---

## 8. 开发工作流

### 修改 Java 代码

```bash
# 1. 在 IDE 中修改代码
# 2. 重新构建并重启对应服务
docker compose up -d --build item-service
# 3. 验证
docker compose logs -f item-service
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
# - 内存不足：给 Docker Desktop 多分配内存（建议 8GB+）
```

### 9.2 数据库初始化失败（表没有创建）

**原因**：MySQL 的 Data Volume 已存在（之前启动过），初始化脚本只在**数据目录首次创建时**执行一次。

```bash
# 解决方法：清除数据卷重新初始化
docker compose down -v
docker compose up -d
```

### 9.3 微服务连不上 Nacos

```bash
# 检查 Nacos 是否就绪
curl http://localhost:8848/nacos/v1/console/health/readiness

# 检查服务是否注册到 Nacos
# 浏览器访问 http://localhost:8848/nacos → 服务管理 → 服务列表

# 检查服务日志中 Nacos 连接信息
docker compose logs item-service | grep nacos
```

### 9.4 网关路由不通

```bash
# 检查网关路由配置
docker compose logs hm-gateway

# 确认目标服务已在 Nacos 注册
# 访问 http://localhost:8848/nacos 查看服务列表

# 直接测试微服务是否正常
curl http://localhost:8082/items/1
```

### 9.5 微服务连不上 MySQL / Redis

```bash
# 检查网络是否正常
docker exec hmall-item-service ping mysql
docker exec hmall-item-service ping redis

# 检查 MySQL 是否就绪
docker exec hmall-mysql mysqladmin ping -uroot -p123

# 检查 Redis 是否就绪
docker exec hmall-redis redis-cli ping
```

### 9.6 编译报错（Lombok 相关）

本项目的 Lombok 已升级到 **1.18.46**，兼容 JDK 26。如果仍有问题：

```bash
# 确认 pom.xml 中的 Lombok 版本
grep "lombok.version" pom.xml
# 应该看到：<org.projectlombok.version>1.18.46</org.projectlombok.version>
```

### 9.7 端口被占用

```bash
# 查看端口占用情况
# Windows:
netstat -ano | findstr :8080
netstat -ano | findstr :8848

# macOS/Linux:
lsof -i :8080
lsof -i :8848

# 修改 docker-compose.yml 中的端口映射（前面的宿主机端口）：
# "8080:8080" 改为 "8088:8080" 等
```

### 9.8 Docker Desktop 内存不足

症状：后端频繁重启，日志显示 `OutOfMemoryError` 或 MySQL 启动超时。

解决：Docker Desktop → Settings → Resources → Memory → 调至 8GB+。
