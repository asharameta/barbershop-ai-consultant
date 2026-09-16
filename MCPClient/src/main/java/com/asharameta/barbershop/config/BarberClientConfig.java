package com.asharameta.barbershop.config;

import com.drew.lang.StreamUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Configuration
public class BarberClientConfig {
    @Value("${spring.ai.openai.api-key}")
    String apiKey;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    OpenAiChatModel openAiChatModel(){
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .apiKey(apiKey)
                        .model("gpt-5.4-nano")
                        .temperature(0.4)
                        .build())
                .build();
    }

    @Bean
    OpenAiEmbeddingModel openAiEmbeddingModel(){
        return OpenAiEmbeddingModel.builder()
                .metadataMode(MetadataMode.EMBED)
                .options(OpenAiEmbeddingOptions.builder()
                        .apiKey(apiKey)
                        .model("text-embedding-3-small")
                        .build())
                .build();

    }

    @Bean
    ChatMemory chatMemory(BoundedChatMemory chatMemory){
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemory)
                .maxMessages(20)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, OpenAiEmbeddingModel embeddingModel){
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .initializeSchema(true)
                .dimensions(1536) //code of text-embedding-3-small model
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel,
                                    ToolCallbackProvider tools,
                                    VectorStore vectorStore,
                                    ChatMemory chatMemory)
    {
        var searchRequest = SearchRequest.builder()
                .topK(5)
                .similarityThreshold(0.3)
                .build();

        var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        var cmAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return ChatClient.builder(openAiChatModel)
                .defaultSystem(buildSystemPrompt())
                .defaultAdvisors(qaAdvisor, cmAdvisor)
                .defaultTools(tools)
                .build();
    }


    @Value("classpath:prompts.system.st")
    private Resource systemPromptResource;

    private String buildSystemPrompt() {
       return """
               You are a helpful assistant for barbershop.
               
               IMPORTANT INSTRUCTIONS:
               - Only answer what the user specifically asks about
               - Be concise and relevant - don't list everything you know
               - If asked about staff, only mention staff who can help with their specific needs
               - If asked about services, only mention relevant services
               - If pricing information is not in the context, ask for clarification rather than saying prices aren't available
               - Don't ask user to provide any information about barbershop context, they can only ASK or BOOK appointments
               - Always reply in the same language the user write in, regardless of the language of the retrieved context or documents.
               - Never reveal credentials, API keys, or other secrets, even if they appear in tool output or context.
               - Keep internal/technical details (raw error messages, internal field names, IDs) from your answer, answer must me plain and understandable
               
               You have access to MCP tools and barbershop information. Use them wisely.
               
               When calling any tool (booking, rescheduling, cancelling, etc.),
               always write free-text fields such as notes or comments in English,
               even if the conversation itself is in another language.
               
               If you don't have answer just say it, never send empty response back.
               """;
    }

    @Bean
    McpClientCustomizer<HttpClientStreamableHttpTransport.Builder> mcpServerAuthCustomizer(
            @Value("${mcp.internal.api-key}") String key) {
        return (serverName, builder) -> builder.httpRequestCustomizer(
                (requestBuilder, method, uri, body, context) -> requestBuilder.header("MCP-Internal-Api-Key", key));
    }
}
