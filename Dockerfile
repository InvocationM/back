# Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 先复制 pom.xml 并下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app

# 复制构建产物
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# 添加 JVM 优化参数
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
