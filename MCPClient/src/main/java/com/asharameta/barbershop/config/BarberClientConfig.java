package com.asharameta.barbershop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.asharameta.barbershop.utils.BarbershopFileParser.BarbershopMetadata;
import static com.asharameta.barbershop.utils.BarbershopFileParser.parseFileName;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Configuration
public class BarberClientConfig {
    @Value("${spring.ai.openai.api-key}")
    String API_KEY;

    @Bean
    OpenAiChatModel openAiChatModel(){
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .apiKey(API_KEY)
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
                        .apiKey(API_KEY)
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
                                    VectorStore vectorStore) {
        String barbershopName = "gentleman";
        String barbershopLocation = "warsaw";
        String barbershopCategory = "booking";

        var searchRequest = SearchRequest.builder()
                .filterExpression("barbershop_category == '" + barbershopCategory + "'")
                .build();

        var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        return ChatClient.builder(openAiChatModel)
                .defaultSystem(buildSystemPrompt(barbershopName, barbershopLocation))
                .defaultAdvisors(qaAdvisor)
                .defaultTools(tools)
                .build();
    }

    private String buildSystemPrompt(String barbershopName, String location) {
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
        """, barbershopName, location);
    }

    @Bean
    CommandLineRunner ingestDocuments(VectorStore vectorStore) {
        return args -> {
            try {
                PathMatchingResourcePatternResolver resolver =
                        new PathMatchingResourcePatternResolver();
                Resource[] resources = resolver.getResources("classpath:/docs/*");

                if (resources.length == 0) {
                    System.err.println("No documents found in /docs/ directory!");
                    return;
                }

                TextSplitter splitter = TokenTextSplitter.builder()
                                                        .withChunkSize(300)
                                                        .withMinChunkSizeChars(100)
                                                        .withMinChunkLengthToEmbed(50)
                                                        .withMaxNumChunks(10000)
                                                        .withKeepSeparator(true)
                                                        .build();
                List<Document> allChunks = Arrays.stream(resources)
                        .flatMap(resource -> {
                            BarbershopMetadata metadata = parseFileName(Objects.requireNonNull(resource.getFilename()));
                            List<Document> documents = new TikaDocumentReader(resource).read();
                            if (metadata != null) {
                                documents.forEach(doc -> {
                                    doc.getMetadata().put("barbershop_name", metadata.barbershopName().toLowerCase());
                                    doc.getMetadata().put("barbershop_city", metadata.city().toLowerCase());
                                    doc.getMetadata().put("barbershop_category", metadata.category().toLowerCase());
                                });
                            }
                            return splitter.split(documents).stream();
                        })
                        .toList();

                vectorStore.add(allChunks);
                System.out.println("Parsed " + allChunks.size() + " chunks into vector store.");

            } catch (Exception e) {
                throw new RuntimeException("Failed to ingest documents into vector store", e);
            }
        };
    }

}
