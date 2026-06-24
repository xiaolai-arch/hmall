# 黑马商城 Docker 部署 — Bug 修复记录

## Bug 清单总览

| # | Bug | 现象 | 根因 | 修复方式 |
|---|-----|------|------|----------|
| 1 | 文件名与类名不匹配 | `类 JsonConfig 是公共的, 应在名为 JsonConfig.java 的文件中声明` | 文件名带时间戳后缀 `JsonConfig_20231023_150602.java` | 重命名为 `JsonConfig.java` |
| 2 | Lombok 版本不兼容 JDK 26 | `ExceptionInInitializerError: TypeTag :: UNKNOWN` | Lombok 1.18.20 仅支持到 JDK 16 | 升级到 1.18.46 |
| 3 | Docker 基础镜像已下架 | `403 Forbidden: openjdk:11.0-jre-buster` | 旧 OpenJDK 镜像已从 Docker Hub 移除 | 改用本地 tar 镜像 |
| 4 | Maven 构建镜像间接依赖下架镜像 | 同上，Maven 镜像也基于已被移除的 openjdk | `maven:3.8-openjdk-11` 底层用了已下架的 openjdk | 换为 `maven:3.8-eclipse-temurin-11` |
| 5 | Docker 尝试拉取本地镜像 | `403 Forbidden: hmall-service:latest` | Docker Compose 默认先 pull 再 build | 添加 `pull_policy: build` |
| 6 | 端口冲突（MySQL） | `bind: address already in use 0.0.0.0:3306` | 宿主机已有 MySQL 占用 3306 | 外部端口改为 3307 |
| 7 | 端口冲突（Redis） | （同上，但被前一个错误遮挡） | 宿主机 Redis + Docker Desktop 占用 6379 | 外部端口改为 6380 |
| 8 | 端口冲突（后端） | `bind: address already in use 0.0.0.0:8080` | 宿主机 Java 进程占用 8080 | 外部端口改为 8081 |
| 9 | Nginx 配置路径拼写错误 | Nginx 启动失败（潜在风险） | `ngnx` 和 `inx` 缺少/写错字母 | 修正为 `./nginx/nginx.conf` |

---

## 详细分析

### Bug 1：文件名与类名不匹配

**现象：**
```
java: 类 JsonConfig 是公共的, 应在名为 JsonConfig.java 的文件中声明
```

**根因：**

文件 `hm-common/src/main/java/com/hmall/common/config/JsonConfig_20231023_150602.java` 的
文件名带有时间戳后缀 `_20231023_150602`（2023年10月23日 15:06:02），这是 IDE 自动生成的
备份文件（可能是某次重构或版本回退时留下的）。

Java 语言规范规定：如果一个类是 `public` 的，那么 `.java` 文件名必须与类名完全一致。
文件中声明的类是 `public class JsonConfig`，所以文件名必须是 `JsonConfig.java`。

**修复：**
```bash
mv JsonConfig_20231023_150602.java JsonConfig.java
```

---

### Bug 2：Lombok 版本不兼容 JDK 26

**现象：**
```
java: java.lang.ExceptionInInitializerError
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

**根因：**

宿主机安装的是 JDK 26，而项目 `pom.xml` 中 Lombok 版本是 **1.18.20**（2021年发布）。
Lombok 作为注解处理器，在编译期会侵入 `com.sun.tools.javac` 内部 API 来生成代码。
每个 JDK 大版本这些内部类的结构都可能发生变化。

| Lombok 版本 | 支持的最高 JDK |
|-------------|---------------|
| 1.18.20 | JDK 16 |
| 1.18.30 | JDK 21 |
| 1.18.32 | JDK 22 |
| 1.18.36 | JDK 24 |
| **1.18.46** | **JDK 26** |

`TypeTag` 是 javac 内部枚举，JDK 26 中它的结构和值与 JDK 11 完全不同，
Lombok 1.18.20 硬编码的常量对不上，初始化时就抛出了 `ExceptionInInitializerError`。

**修复：**

`pom.xml` 第 28 行：
```xml
<!-- 改前 -->
<org.projectlombok.version>1.18.20</org.projectlombok.version>
<!-- 改后 -->
<org.projectlombok.version>1.18.46</org.projectlombok.version>
```

---

### Bug 3：Docker 基础镜像已下架

**现象：**
```
failed to solve: openjdk:11.0-jre-buster: failed to resolve source metadata
unexpected status from HEAD request: 403 Forbidden
```

**根因：**

Docker 官方在 2021 年宣布废弃 `openjdk` 命名空间，所有 OpenJDK 镜像迁移到了
`eclipse-temurin` 命名空间。旧的 `openjdk:11.0-jre-buster` 标签已经从 Docker Hub
彻底移除。用户配置的 daocloud.io 镜像加速器也同步移除了该镜像，返回 403。

**修复：**

项目 `images/` 目录下已经打包好了三个 Docker 镜像 tar 文件：
```bash
docker load -i images/jdk.tar    # → openjdk:11.0-jre-buster（加载到本地）
docker load -i images/mysql.tar  # → mysql:latest
docker load -i images/nginx.tar  # → nginx:latest
```

加载后 Docker 检测到本地已有该镜像，不会再尝试从远程拉取。

> **备选方案**：如果想用维护中的镜像，可改为 `eclipse-temurin:11-jre`。
> 但考虑到 `images/jdk.tar` 已提供，直接加载本地包是最稳妥的方式。

---

### Bug 4：Maven 构建镜像间接依赖已下架镜像

**现象：**

与 Bug 3 类似，如果 `maven:3.8-openjdk-11` 也无法拉取。

**根因：**

`maven:3.8-openjdk-11` 的底层基础镜像同样基于已被移除的 `openjdk` 命名空间。
虽然 Maven 团队后续发布了兼容版本，但旧标签或某些镜像加速器可能仍有问题。

**修复：**

`hm-service/Dockerfile` 第 2 行：
```dockerfile
# 改前
FROM maven:3.8-openjdk-11 AS builder
# 改后
FROM maven:3.8-eclipse-temurin-11 AS builder
```

`eclipse-temurin` 是 OpenJDK 的官方继任者，持续维护更新，不会被下架。

---

### Bug 5：Docker 尝试从远程拉取本地构建的镜像

**现象：**
```
! Image h... unknown: failed to resolve reference
"docker.io/library/hmall-service:latest": 403 Forbidden
```

**根因：**

`hmall-service:latest` 是通过 `docker compose build` 在本地构建的镜像，
Docker Hub 上不存在。但 Docker Compose 的默认行为是：先尝试 `pull`，
拉取失败后才执行 `build`。由于配置了 daocloud.io 加速器，
Docker 会去远程查询 `library/hmall-service`，得到 403，然后才回退到本地构建。

这本身不是致命错误（警告而已），但每次启动都会等待 121 秒超时，
严重影响开发体验。

**修复：**

`docker-compose.yml` 中给 `hmall` 服务添加：
```yaml
build:
  context: .
  dockerfile: ./hm-service/Dockerfile
  pull: false          # 构建阶段也不拉取基础镜像（已有本地缓存）
image: hmall-service:latest
pull_policy: build     # 总是本地构建，不尝试远程拉取
```

---

### Bug 6–8：端口冲突（3306 / 6379 / 8080）

**现象：**
```
Error response from daemon: ports are not available:
exposing port TCP 0.0.0.0:3306: listen tcp 0.0.0.0:3306: bind: address already in use
```

**根因：**

宿主机上已经运行着 MySQL、Redis 和一个 Java 进程，占用了默认端口：

| 端口 | 占用者 | 证明 |
|------|--------|------|
| 3306 | `mysqld` | `lsof -i :3306` → 本地 MySQL 实例 |
| 6379 | `redis-server` + `com.docker` | `lsof -i :6379` → 本地 Redis + Docker Desktop |
| 8080 | `java` | `lsof -i :8080` → 本地 Java 进程 |

Docker 容器的端口映射是将宿主机端口转发到容器内部端口，
当宿主机端口已被占用时，映射就会失败。

> **注意**：容器**内部**端口不变——MySQL 在容器内仍然是 3306，
> Redis 仍然是 6379，后端仍然是 8080。容器间通过 Docker 内部网络通信，
> 不经过宿主机端口映射。外部端口只影响从宿主机/浏览器访问。

**修复：**

`docker-compose.yml` 中修改外部端口映射：

```yaml
# MySQL:  3306:3306 → 3307:3306
# Redis:  6379:6379 → 6380:6379
# 后端:   8080:8080 → 8081:8080
```

最终访问地址：
| 服务 | 地址 |
|------|------|
| 后端 API | http://localhost:8081/hi |
| 用户端 | http://localhost:18080 |
| 管理端 | http://localhost:18081/users.html |

---

### Bug 9：Nginx 配置路径拼写错误

**现象：**

如果未修复，Nginx 容器启动后会因为找不到配置文件而使用默认配置，
导致前端页面无法访问或 API 代理不生效。

**根因：**

手动编辑 `docker-compose.yml` 时，`nginx` 单词拼写错误：
- 第一次误写为 `ngnx`（少了一个 `i`）
- 第二次误写为 `inx`（少了 `ng`）

导致 Volume 挂载路径指向不存在的目录，Nginx 找不到自定义配置。

**修复：**

```yaml
# 错误
- ./ngnx/nginx.conf:/etc/nginx/nginx.conf
- .inx/nginx.conf:/etc/nginx/nginx.conf

# 正确
- ./nginx/nginx.conf:/etc/nginx/nginx.conf
```

---

## 修改文件汇总

| 文件 | 改动 |
|------|------|
| `hm-common/.../JsonConfig_20231023_150602.java` | 重命名为 `JsonConfig.java` |
| `pom.xml` | Lombok `1.18.20` → `1.18.46` |
| `hm-service/Dockerfile` | 多阶段构建；Maven 镜像换 `eclipse-temurin`；运行镜像用本地 tar |
| `hm-service/src/main/resources/application-dev.yaml` | 新增 `spring.redis.host: redis` |
| `docker-compose.yml` | 端口映射、`pull_policy`、路径修正 |
| `.dockerignore`（新建） | 排除 IDE 配置和构建产物 |
| `DOCKER.md`（新建） | Docker 部署完整指南 |