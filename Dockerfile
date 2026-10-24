# ==========================================
# Estágio 1: Build & Resolução de Dependências
# ==========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Cache de dependências: copia apenas o pom.xml primeiro
COPY pom.xml .

# Baixa as dependências e plugins para cache do Docker
RUN mvn dependency:go-offline -B || true

# 2. Copia o código-fonte e empacota o artefato
COPY src ./src
RUN mvn clean package -DskipTests -B

# 3. Extrai dependências do Spring Boot para cache otimizado de camadas
RUN java -Djarmode=tools -jar target/*.jar extract --destination target/extracted

# ==========================================
# Estágio 2: Imagem Final de Produção (Runtime Seguro)
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Cria usuário e grupo não-root para execução segura e ajusta permissões do diretório de trabalho
RUN addgroup -S -g 1001 appgroup && \
    adduser -S -u 1001 -G appgroup appuser && \
    chown -R appuser:appgroup /app

# Copia dependências (camada pesada que raramente muda)
COPY --from=builder --chown=appuser:appgroup /build/target/extracted/lib/ ./lib/

# Copia o JAR da aplicação (camada leve atualizada a cada build)
COPY --from=builder --chown=appuser:appgroup /build/target/extracted/*.jar ./app.jar

# Define permissões e usuário não-root
USER appuser:appgroup

# Expõe a porta padrão da aplicação
ENV SERVER_PORT=8080
EXPOSE 8080

# Otimizações de memória e recursos para containers
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-jar", "app.jar"]