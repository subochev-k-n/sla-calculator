# ---- Build stage ----
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
RUN chmod +x ./gradlew \
    && WRAPPER_JAR=gradle/wrapper/gradle-wrapper.jar \
    && if [ ! -f "$WRAPPER_JAR" ]; then \
         WRAPPER_VERSION=$(grep distributionUrl gradle/wrapper/gradle-wrapper.properties | sed 's/.*gradle-\(.*\)-bin.zip/\1/') \
         && wget -q -O "$WRAPPER_JAR" "https://raw.githubusercontent.com/gradle/gradle/v${WRAPPER_VERSION}/gradle/wrapper/gradle-wrapper.jar"; \
       fi \
    && ./gradlew bootJar --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar", "--server.port=8080"]
