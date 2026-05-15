# scraper-service

Fetches job listings from public APIs and stores them in PostgreSQL.

## Responsibilities

- Fetches raw job listings from the [RemoteOK public API](https://remoteok.com/api)
- Persists new listings into the `raw_listings` table (idempotent — duplicates are skipped)
- Exposes a REST API for browsing listings and triggering scrapes on demand
- In Phase 2 this service will publish to the `raw-listings` Kafka topic instead of writing directly to Postgres

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/scraper/trigger` | Fetch latest listings from RemoteOK and store new ones |
| `GET` | `/api/v1/listings` | Paginated list of stored listings |
| `GET` | `/api/v1/listings/{id}` | Get a single listing by UUID |
| `GET` | `/swagger-ui.html` | Interactive API docs |
| `GET` | `/v3/api-docs` | OpenAPI JSON spec |
| `GET` | `/actuator/health` | Health check |

## Running locally

**Prerequisites:** Docker Desktop running.

```bash
# 1. Start Postgres
docker compose -f infra/docker-compose.yml up -d

# 2. Run the service
./gradlew :scraper-service:bootRun

# 3. Trigger a scrape
curl -X POST http://localhost:8080/api/v1/scraper/trigger

# 4. Browse listings
curl http://localhost:8080/api/v1/listings
```

Swagger UI: http://localhost:8080/swagger-ui.html

## Configuration

| Property | Env var | Default |
|----------|---------|---------|
| Database URL | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/jobsignal` |
| RemoteOK base URL | `REMOTEOK_BASE_URL` | `https://remoteok.com` |
| RemoteOK timeout | `REMOTEOK_TIMEOUT_SECONDS` | `10` |
| Server port | `SERVER_PORT` | `8080` |

## Testing

```bash
# Unit tests only
./gradlew :scraper-service:test --tests "com.jobsignal.scraper.unit.*"

# Integration tests (requires Docker)
./gradlew :scraper-service:test --tests "com.jobsignal.scraper.integration.*"

# Contract tests (requires Docker)
./gradlew :scraper-service:test --tests "com.jobsignal.scraper.contract.*"

# All tests
./gradlew :scraper-service:test
```

## Architecture

```
api/
  ListingController     GET /listings, GET /listings/{id}
  ScraperController     POST /scraper/trigger
  dto/                  ListingResponse, PagedResponse, ScrapeResultResponse

service/
  ListingScraperService orchestrates fetch + dedup + save

client/
  RemoteOkClient        interface
  RemoteOkClientImpl    WebClient-based implementation

persistence/
  entity/RawListingEntity
  repository/RawListingRepository

exception/
  GlobalExceptionHandler    @RestControllerAdvice
  ListingNotFoundException

config/
  OpenApiConfig         SpringDoc OpenAPI bean
  WebClientConfig       RemoteOK WebClient bean
  RemoteOkProperties    @ConfigurationProperties
```
