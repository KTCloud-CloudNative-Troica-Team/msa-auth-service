# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY auth/build.gradle.kts auth/
COPY auth-service/build.gradle.kts auth-service/
RUN chmod +x gradlew

ARG GPR_USER
ARG GPR_TOKEN
RUN if [ -n "$GPR_USER" ] && [ -n "$GPR_TOKEN" ]; then \
      mkdir -p /root/.gradle && \
      echo "gpr.user=$GPR_USER" > /root/.gradle/gradle.properties && \
      echo "gpr.token=$GPR_TOKEN" >> /root/.gradle/gradle.properties ; \
    fi
RUN ./gradlew dependencies --no-daemon || true

COPY auth/src auth/src
COPY auth-service/src auth-service/src
RUN ./gradlew :auth-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS layers
WORKDIR /app
COPY --from=build /app/auth-service/build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app

COPY --from=layers /app/dependencies/ ./
COPY --from=layers /app/spring-boot-loader/ ./
COPY --from=layers /app/snapshot-dependencies/ ./
COPY --from=layers /app/application/ ./

EXPOSE 8005 9005
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
