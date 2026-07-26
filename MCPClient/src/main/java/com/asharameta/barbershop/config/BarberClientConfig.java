package com.asharameta.barbershop.config;

import com.asharameta.barbershop.knowledgebase.KnowledgeBaseLoader;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.Locale;


@Configuration
public class BarberClientConfig {
    @Value("${spring.ai.openai.api-key}")
    String apiKey;

    @Value("${asharameta.barbershop.name}")
    String barbershopName;

    @Value("${asharameta.barbershop.city}")
    String barbershopCity;

    @Value("${asharameta.barbershop.knowledge-base.resource-pattern}")
    String resourcePattern;

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
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

    @Bean
    public VectorStore vectorStore(OpenAiEmbeddingModel openAiEmbeddingModel){
        return SimpleVectorStore.builder(openAiEmbeddingModel).build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel,
                                    ToolCallbackProvider tools,
                                    VectorStore vectorStore)
    {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        var searchRequest = SearchRequest.builder()
                .filterExpression(b.and(
                        b.in("barbershop_name", barbershopName.toLowerCase(Locale.ROOT)),
                        b.in("barbershop_city", barbershopCity.toLowerCase(Locale.ROOT))
                ).build())
                .topK(5)
                .similarityThreshold(0.3)
                .build();

        var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        return ChatClient.builder(openAiChatModel)
                .defaultSystem(buildSystemPrompt())
                .defaultAdvisors(qaAdvisor)
                .defaultTools(tools)
                .build();
    }

    private String buildSystemPrompt() {
        return String.format("""
        You are a helpful assistant for %s barbershop.
        Location: %s
        
        Opening hours: Monday-Friday 8:00-21:00 | Saturday-Sunday 08:00-15:00

        IMPORTANT INSTRUCTIONS:
        - Only answer what the user specifically asks about
        - Be concise and relevant - don't list everything you know
        - If asked about staff, only mention staff who can help with their specific needs
        - If asked about services, only mention relevant services
        - If pricing information is not in the context, ask for clarification rather than saying prices aren't available

        You have access to MCP tools and barbershop information. Use them wisely.
        
        If you don't have answer just say it, never send empty response back.
        """, barbershopName, barbershopCity);
    }

    @Bean
    public TextSplitter splitter(){
        return TokenTextSplitter.builder()
                .withChunkSize(300)
                .withMinChunkSizeChars(100)
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
