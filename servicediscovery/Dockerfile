# Estágio 1: Build - Usando a imagem JDK padrão e completa da Eclipse Temurin
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test --no-daemon

# Estágio 2: Runtime - Usando a imagem JRE padrão e completa da Eclipse Temurin
FROM eclipse-temurin:21-jre
WORKDIR /app

# O `*` ajuda a não se preocupar com a versão exata do JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# A porta será diferente para cada serviço
# Altere para 8081 no gateway, 8082 na recomendação, 8761 no discovery e 8080 no booking
EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]