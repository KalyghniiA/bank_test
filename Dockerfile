FROM maven:4.0.0-rc-4-eclipse-temurin-21-alpine AS builder
LABEL authors="aleksandrkaluznyj"
WORKDIR /Bank

COPY pom.xml .
COPY /src ./src
COPY checkstyle.xml .
COPY suppressions.xml .

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /Bank

COPY --from=builder /Bank/target/Bank-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]