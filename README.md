# Barbershop AI Consultant

An AI-powered REST API chatbot for a barbershop that answers questions
about services, pricing, and availability by pulling responses from a
real knowledge base (txt files) instead of making things up.
Also handles appointment booking stored in PostgreSQL.


## Architecture

Two modules that run independently:

- MCPServer — exposes the barbershop knowledge base and booking 
  logic via Model Context Protocol (MCP).
  Tools: `sayHello`, `bookAppointment`, 
  `cancelAppointment`, `getClientAppointments`, `getBarberSchedule`.
  
- MCPClient — Spring Boot REST API, handles user queries, 
  retrieves context via RAG (SimpleVectorStore), calls OpenAI to generate responses.

## Tech Stack
Java 21, Spring Boot, Spring AI, OpenAI API, RAG, MCP, PostgreSQL, Gradle

## How to Run

**0. Create `.env` file in project root**
```env
OPENAI_API_KEY=your-key-here
POSTGRES_URL=your-url-here
POSTGRES_USERNAME=your-username-here
POSTGRES_PASSWORD=your-password-here
```

You can also find `.env.example` file in project root

**1. Build and start**
```bash
./start.sh
```

or just straight

```powershell
./gradlew bootJar; docker compose up -d --build
```

**2. Send a query**
```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What haircut services do you offer?"}'
```

## Why RAG?
Without RAG, the model would hallucinate barbershop-specific details 
(pricing, services, hours). RAG ensures every answer is taken from knowledge base (txt files in my case)
