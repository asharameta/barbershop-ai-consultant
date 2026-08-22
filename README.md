![CI](https://github.com/asharameta/barbershop-ai-consultant/actions/workflows/ci.yml/badge.svg)

# Barbershop AI Consultant

An AI-powered REST API chatbot for a barbershop that answers questions
about services, pricing, and availability by pulling responses from a
real knowledge base (txt files) instead of making things up.
Also handles appointment booking stored in PostgreSQL.


## Architecture

Two modules that run independently:

- MCPServer — exposes the barbershop knowledge base and booking 
  logic via Model Context Protocol (MCP).
  Tools: `bookAppointment`, `cancelAppointment`, `getClientAppointments`, `getBarberSchedule`.
  
- MCPClient — Spring Boot REST API, handles user queries,
  retrieves context via RAG (PGVector), filtered per-barbershop from metadata, calls OpenAI to generate responses.

```mermaid
flowchart TD
    User(["Client / curl"]) -->|"POST /chat"| Controller["BarberClientController"]

    subgraph MCPClient["MCPClient (:8080)"]
        Controller --> ChatClient["ChatClient\n(OpenAI gpt-5.4-nano)"]
        ChatClient --> QAAdvisor["QuestionAnswerAdvisor\n(filter: barbershop_name + city)"]
        ChatClient -->|tool calls| ToolProvider["ToolCallbackProvider\n(MCP client, SSE)"]

        Loader["KnowledgeBaseLoader\n(startup, .txt docs)"] --> Splitter["TokenTextSplitter"]
        Splitter --> Embed["OpenAiEmbeddingModel\n(text-embedding-3-small)"]
    end

    subgraph MCPServer["MCPServer (:8081)"]
        ToolProvider -->|SSE| Tools["MCP Tools\nbookAppointment / cancelAppointment / rescheduleAppointment /\ngetClientAppointments / getBarberSchedule"]
        Tools --> BarberService["BarberService"]
        BarberService --> DAO["AppointmentDAO"]
    end

    subgraph DB["PostgreSQL + pgvector"]
        Vectors[("PGVector store\nembedded knowledge base")]
        Appointments[("appointments table\nFlyway-migrated")]
    end

    QAAdvisor <--> Vectors
    Embed --> Vectors
    DAO <--> Appointments

    ChatClient -->|answer| Controller
    Controller -->|Answer JSON| User
```

## Tech Stack
Java 21, Spring Boot 4.1.0, Spring AI 2.0, OpenAI API, RAG, MCP, PostgreSQL, Gradle

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
  -d '{
        "question": "Can you list staff that works in this barbershop?",
        "barbershopName": "STARY_CYRULIK",
        "city": "Gdansk"
      }'
```

## Why RAG?
Without RAG, the model would hallucinate barbershop-specific details 
(pricing, services, hours). RAG ensures every answer is taken from knowledge base (txt files in my case)
