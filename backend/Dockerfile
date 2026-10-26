# =============================================================================
# Estágio 1: Build da aplicação com Maven e Java 21
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copia pom.xml para cache de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código-fonte e compila o pacote
COPY src ./src
RUN mvn clean package -DskipTests -B

# =============================================================================
# Estágio 2: Imagem final de execução com JRE 21 Alpine enxuto
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuário não-root por segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copia o artefato compilado
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

ENV TZ=America/Sao_Paulo \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
