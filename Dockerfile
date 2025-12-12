# 构建阶段：使用 Maven 编译
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn dependency:go-offline

RUN mvn clean package -DskipTests

# 运行阶段：使用轻量级 JDK
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]