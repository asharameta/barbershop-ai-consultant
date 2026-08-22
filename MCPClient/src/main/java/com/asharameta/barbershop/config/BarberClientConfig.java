package com.asharameta.barbershop.config;

import com.asharameta.barbershop.knowledgebase.KnowledgeBaseLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;


@Configuration
public class BarberClientConfig {
    @Value("${spring.ai.openai.api-key}")
    String apiKey;

    @Value("${asharameta.barbershop.knowledge-base.resource-pattern}")
    String resourcePattern;

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

        You have access to MCP tools and barbershop information. Use them wisely.
        
        If you don't have answer just say it, never send empty response back.
        """;
    }

    @Bean
    public TextSplitter splitter(){
        return TokenTextSplitter.builder()
                .withChunkSize(1000)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(50)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();
    }

    @Bean
    public KnowledgeBaseLoader knowledgeBaseLoader(TextSplitter splitter, ResourcePatternResolver resolver){
        return new KnowledgeBaseLoader(resourcePattern, splitter, resolver);
    }

    @Bean
    CommandLineRunner ingestDocuments(VectorStore vectorStore, KnowledgeBaseLoader knowledgeBaseLoader) {
        return args -> {
            vectorStore.add(knowledgeBaseLoader.loadDocuments());
        };
    }

}
