# ---- Build: dung Maven de dong goi file jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Run: chi can JRE, image nho hon ----
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Ho_Chi_Minh
COPY --from=build /app/target/dtms-backend-*.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
