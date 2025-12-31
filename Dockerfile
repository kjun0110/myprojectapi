# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
RUN apk add --no-cache bash unzip

# Gradle wrapper 복사
COPY gradlew .
COPY gradlew.bat .
COPY gradle ./gradle
RUN chmod +x ./gradlew

# 빌드 파일 복사
COPY build.gradle .
COPY settings.gradle .

# 소스 코드 복사 (Gateway, OAuth Service, User Service 모두 포함)
COPY src ./src

# 빌드 실행
RUN ./gradlew bootJar --no-daemon --stacktrace

# Runtime stage
FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
