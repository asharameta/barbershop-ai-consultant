![CI](https://github.com/asharameta/barbershop-ai-consultant/actions/workflows/ci.yml/badge.svg)

# Java Spring AI Booking Assistant

A Java 21 and Spring Boot backend combining RAG, MCP and automated appointment booking for barbershops.

The system combines:
- Spring Boot REST API
- Spring AI and RAG with PostgreSQL/pgvector
- Model Context Protocol (MCP)
- Redis and Caffeine caching
- API key authentication and rate limiting
- Idempotent request handling
- Docker-based deployment

## Architecture

A single modulith(modular monolith) Spring Boot application:

- `chat` — REST endpoint `POST /api/v1/chat`, `ChatClient` wiring (RAG advisor + bounded chat memory + tools),
  chat history per `conversationId` in a bounded in-memory cache (Caffeine, evicted after 1h / max conversations).
- `knowledgebase` — loads the txt knowledge base into PGVector on startup
- `appointment` — booking logic in PostgreSQL (Flyway migrations).
- `ai` — chat / embedding provider selection.
- `security` — API key and CORS.
- `ratelimit` — Redis and Bucket4j limiter per API key / client IP.
- `idempotency` — Redis `Idempotency-Key` handling for `/chat` and request body size limit.
- `common` — shared Redis configuration.
  
## Tech Stack

Java 21, Spring Boot 4.1.0, Spring AI 2.0, OpenAI API, RAG, MCP, PostgreSQL (PGVector, Flyway), Bucket4j, Caffeine, Redis, Gradle

## How to Run

**0. Create `.env` file in project root**

Copy `.env.example` to `.env` and fill with your own data.

**1. Build and start**

```bash
./start.sh
```

or just straight

```powershell
./gradlew clean bootJar && docker compose build --no-cache && docker compose up -d
```

**2. Send a query**

Every request needs a `B-API-Key` header (matching `BARBERSHOP_API_KEY`), a `conversationId`
(a UUID, used to keep chat history scoped per conversation), and an `Idempotency-Key` header.

```bash
curl -X POST http://localhost:8080/api/v1/barbershops/{cechownia}/chat \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: idempotency-key-here' \
  -H 'B-API-Key: your-api-key-here' \
  -d '{
        "conversationId": "conversation-id-here",
        "question": "Can you list staff that works in this barbershop?",
        "barbershopCity": "Gdansk"
      }'
```
