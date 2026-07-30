# 멀티스테이지 빌드 — Gradle 빌드 후 JRE 17 런타임 이미지
FROM gradle:8.10-jdk17 AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN gradle clean bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
# 업로드 볼륨(웹루트 밖, R-U1-21)
RUN mkdir -p /app/var/uploads
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
