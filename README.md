# Barbershop AI Consultant

An AI-powered REST API chatbot for a barbershop that answers questions about services, pricing, and availability by pulling responses from a real knowledge base (txt files) instead of making things up.


## Architecture
- MCPServer — exposes barbershop knowledge via MCP (Model Context Protocol)
- MCPClient — Spring Boot REST API, handles user queries, 
  retrieves context via RAG (SimpleVectorStore), calls OpenAI

## Tech Stack
Java 21, Spring Boot, Spring AI, OpenAI API, MCP, RAG, Gradle

## How to Run
1. Start MCPServer first
2. Start MCPClient
3. POST /chat with your question

## Why RAG?
Without RAG, the model would hallucinate barbershop-specific details 
(pricing, services, hours). RAG ensures every answer is taken from knowledge base (txt files in my case)
