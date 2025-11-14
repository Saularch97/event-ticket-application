FROM eclipse-temurin:22-jdk AS builder
WORKDIR /app
COPY . .

RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:24-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
