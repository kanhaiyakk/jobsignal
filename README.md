# JobSignal

Event-driven backend pipeline that turns raw job listings into AI-generated market intelligence.

## Tech Stack
- Java 21, Spring Boot 3.3.5
- PostgreSQL 16 with pgvector
- Apache Kafka (Phase 2+)
- Google Gemini for AI enrichment (Phase 3+)
- Docker Compose
- OpenAPI / Swagger

## Status
🚧 In active development — see commit history for progress.

## Running locally

```bash
# Start Postgres
cd infra && docker compose up -d

# Run the scraper service
./gradlew :scraper-service:bootRun
```

API docs available at `http://localhost:8080/swagger-ui.html` once running.

## Architecture
Microservices communicating via Kafka. See individual service READMEs for details.

---
*Personal portfolio project.*
