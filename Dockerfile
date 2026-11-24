# ─────────────────────────────────────────────
# ETAPA 1: Build (compilar la aplicación)
# Imagen oficial de Maven + JDK 17: no hace falta instalar Maven con apt-get
# ─────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copia el descriptor de dependencias primero (aprovecha el caché de capas de Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copia el código fuente y compila (los tests ya corrieron en el pipeline)
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─────────────────────────────────────────────
# ETAPA 2: Runtime (imagen final, más liviana)
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre

# Usuario sin privilegios con UID numérico (Kubernetes necesita un UID numérico
# para verificar runAsNonRoot). EKS y ECS no deben ejecutar la app como root.
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app

WORKDIR /app

# Solo copia el .jar generado en la etapa anterior
COPY --from=build /app/target/bdget-*.jar app.jar

USER 10001

# Puerto que expone la aplicación
EXPOSE 8080

# La JVM ajusta el heap al límite de memoria del contenedor (pod de EKS / tarea de ECS)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

# Health check para Docker local. En ECS lo define la task definition (ecs/td-app.tpl.json)
# y en Kubernetes los probes (k8s/20-app.yaml), con este mismo endpoint de Actuator.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fs http://localhost:8080/actuator/health/liveness || exit 1

# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]
