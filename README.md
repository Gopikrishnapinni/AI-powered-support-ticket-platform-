# AI-Powered Support Ticket Platform

Production-oriented support ticket API built with Java 21, Spring Boot, PostgreSQL, JWT authentication, role-based access control, and an OpenAI-compatible LLM integration.

## Features

- JWT login and registration with `USER`, `AGENT`, and `ADMIN` roles.
- Ticket lifecycle management with ownership and role-based authorization.
- AI classification for category, priority, sentiment, and suggested response.
- PostgreSQL persistence with Flyway migrations and indexed ticket queries.
- Actuator health/metrics and Prometheus-compatible metrics.
- Docker Compose for local development and Kubernetes manifests for deployment.
- GitHub Actions workflow for tests, image publishing, and Kubernetes deployment.

## Quick start

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`. Swagger-style endpoint documentation is available through the REST controller annotations; the principal endpoints are:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/tickets
POST /api/v1/tickets
GET  /api/v1/tickets/{id}
PATCH /api/v1/tickets/{id}/status
POST /api/v1/tickets/{id}/ai/analyze
GET  /actuator/health
GET  /actuator/prometheus
```

Set `AI_API_KEY` to enable live LLM analysis. Without a key, the application uses a deterministic local classifier so ticket creation remains available in development.

## Configuration

See [`application.yml`](src/main/resources/application.yml) for all environment variables. `JWT_SECRET` must be a unique secret of at least 32 characters in non-development environments.

## Local development

```bash
docker compose up -d postgres
mvn spring-boot:run
mvn test
```

## Kubernetes

Build and publish the image, then update the image reference in [`deployment.yaml`](k8s/deployment.yaml):

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.example.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

The Kubernetes secret is an example only; use an external secret manager for production credentials.
