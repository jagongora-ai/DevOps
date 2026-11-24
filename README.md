# mysql-aws-3

Microservicio **API REST de Estudiantes** (Spring Boot 3.3.7, Java 17, MySQL 8) con todo lo necesario para:

- **Calidad:** tests (JUnit 5 + Mockito + H2), cobertura con **JaCoCo** y análisis en **SonarCloud**.
- **Seguridad:** escaneo de dependencias e imagen con **Snyk**.
- **Contenedores:** **Docker** multi-etapa (usuario no root) y `docker-compose` para local.
- **Despliegue en AWS (guía 3.2.2 opción B):** ECS Fargate + Service Connect + CloudWatch (`ecs/`).
  La opción A (EKS + Istio) está en `k8s/`.

## Ejecutar en local

```bash
mvn test                  # usa H2 en memoria, no necesita MySQL
docker compose up --build # MySQL + API en http://localhost:8080
```

Endpoints: `GET/POST /students`, `GET/PUT/DELETE /students/{id}`,
`/actuator/health`, `/swagger-ui.html`.

Códigos HTTP: `201` creado · `400` validación (el nombre solo admite letras, 2–50) ·
`404` no existe · `503` base de datos caída (alimenta la alarma 5XX de CloudWatch).

## Estructura

```
mysql-aws-3/
├── src/                      código de la API y tests
├── Dockerfile                build multi-etapa, usuario no root, HEALTHCHECK
├── .dockerignore
├── docker-compose.yml        MySQL + API
├── k8s/                      EKS + Istio (opción A)
├── ecs/                      ECS Fargate (opción B)
│   ├── *.tpl.json            plantillas: task definitions, servicios, dashboard
│   ├── render.sh             genera ecs/out/*.json con tus valores
│   └── check-vars.sh         semáforo: verifica herramientas, credenciales y variables
└── .github/workflows/
    ├── main.yml              CI: tests → JaCoCo → SonarCloud → Snyk → Docker → DockerHub
    ├── deploy-ecs.yml        despliegue manual a ECS Fargate
    └── deploy-eks.yml        despliegue manual a EKS
```

## Secretos de GitHub (Settings → Secrets and variables → Actions)

| Secreto | Usado por |
|---|---|
| `SONAR_TOKEN`, `SONAR_PROJECT_KEY`, `SONAR_ORGANIZATION` | `main.yml` (SonarCloud) |
| `SNYK_TOKEN` | `main.yml` (Snyk) |
| `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` | `main.yml` y los despliegues |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | `deploy-ecs.yml`, `deploy-eks.yml` |

`GITHUB_TOKEN` lo entrega GitHub automáticamente: **no** se crea.

## Notas

- Las credenciales de MySQL son **de laboratorio** (las mismas de `docker-compose.yml`). En un entorno real usa AWS Secrets Manager.
- MySQL usa almacenamiento efímero: los datos se pierden al recrear la tarea o el pod.
- Compila a bytecode Java 17 (`maven.compiler.release=17`); el CI usa JDK 21 solo porque el scanner de SonarCloud lo exige.
- Antes de subir el proyecto, edita en `pom.xml` los valores `TU_ORGANIZACION` (solo se usan en análisis local; el pipeline los toma de los secretos).
