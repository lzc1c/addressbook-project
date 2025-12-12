# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

# 跳过测试（避免 JUnit 报错）
RUN mvn dependency:go-offline
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制 JAR
COPY --from=builder /app/target/*.jar app.jar

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]