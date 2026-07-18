# ========== 阶段1：构建（Maven 编译打包） ==========
FROM maven:3.8-eclipse-temurin-11 AS builder

WORKDIR /build

# 复制根 POM 和所有模块 POM（Maven reactor 解析需要所有模块的 pom.xml 存在）
COPY pom.xml ./
COPY hm-common/pom.xml ./hm-common/
COPY hm-api/pom.xml ./hm-api/
COPY hm-service/pom.xml ./hm-service/
COPY item-service/pom.xml ./item-service/
COPY cart-service/pom.xml ./cart-service/
COPY trade-service/pom.xml ./trade-service/
COPY user-service/pom.xml ./user-service/
COPY pay-service/pom.xml ./pay-service/
COPY hm-gateway/pom.xml ./hm-gateway/

COPY settings.xml /root/.m2/settings.xml

# 下载依赖（这层会被缓存，除非 pom.xml 变化）
RUN mvn dependency:go-offline -B

# 复制源码
COPY hm-common/src ./hm-common/src
COPY hm-api/src ./hm-api/src
COPY pay-service/src ./pay-service/src

# 编译打包（跳过测试，加速构建）
RUN mvn clean package -pl hm-common,hm-api,pay-service -DskipTests -B

# ========== 阶段2：运行（精简 JRE 镜像） ==========
FROM eclipse-temurin:11-jre

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 从构建阶段复制 jar 包
COPY --from=builder /build/pay-service/target/pay-service.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
