# ========== 阶段1：构建（Maven 编译打包） ==========
FROM maven:3.8-eclipse-temurin-11 AS builder

WORKDIR /build

# 先复制父 POM 和模块 POM，利用 Docker 缓存层加速
COPY pom.xml ./
COPY hm-common/pom.xml ./hm-common/
COPY hm-service/pom.xml ./hm-service/

# 下载依赖（这层会被缓存，除非 pom.xml 变化）
RUN mvn dependency:go-offline -B

# 复制源码
COPY hm-common/src ./hm-common/src
COPY hm-service/src ./hm-service/src

# 编译打包（跳过测试，加速构建）
RUN mvn clean package -pl hm-common,hm-service -DskipTests -B

# ========== 阶段2：运行（精简 JRE 镜像） ==========
FROM eclipse-temurin:11-jre

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 从构建阶段复制 jar 包
COPY --from=builder /build/hm-service/target/hm-service.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]