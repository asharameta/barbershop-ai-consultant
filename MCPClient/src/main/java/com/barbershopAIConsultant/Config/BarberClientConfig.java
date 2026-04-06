package com.barbershopAIConsultant.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.barbershopAIConsultant.Utils.BarbershopFileParser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
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
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.ArrayList;
import java.util.List;

import static com.barbershopAIConsultant.Utils.BarbershopFileParser.parseFileName;

@Configuration
public class BarberClientConfig {
    @Value("${spring.ai.openai.api-key}")
    String API_KEY;

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

    @Bean
    public EmbeddingModel embeddingModel(OpenAiApi openAiApi){
        return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model("text-embedding-3-small")
                        .build()
        );
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel){
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .apiKey(API_KEY)
                .baseUrl("https://api.openai.com")
                .build();
    }

    @Bean
    public OpenAiChatModel chatModel(OpenAiApi openAiApi) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-5.4-nano")
                .temperature(1.0)
                .topP(1.0)
                .maxCompletionTokens(1000)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean
    public ChatClient chatClientMCP(OpenAiChatModel chatModel,
                                    ToolCallbackProvider tools,
                                    VectorStore vectorStore) {
        String barbershopName = "gentelman";
        String barbershopLocation = "warsaw";
        String barbershopCategory = "booking";

        var searchRequest = SearchRequest.builder()
                .filterExpression("barbershop_category == '" + barbershopCategory + "'")
                .build();

        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(tools.getToolCallbacks())
                .defaultAdvisors(
                        loggingAdvisor(searchRequest),       // logs filter
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(searchRequest)
                                .build()
                )
                .defaultSystem(buildSystemPrompt(barbershopName, barbershopLocation))
                .build();
    }

    private CallAdvisor loggingAdvisor(SearchRequest searchRequest) {
        return new CallAdvisor() {
            @Override
            public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
                return callAdvisorChain.nextCall(chatClientRequest);
            }

            @Override
            public String getName() { return "LoggingAdvisor"; }

            @Override
            public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
        };
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

                List<Document> allChunks = new ArrayList<>();
                TextSplitter splitter = TokenTextSplitter.builder()
                                                        .withChunkSize(300)
                                                        .withMinChunkSizeChars(100)
                                                        .withMinChunkLengthToEmbed(50)
                                                        .withMaxNumChunks(10000)
                                                        .withKeepSeparator(true)
                                                        .build();
                for (Resource resource : resources) {
                    String filename = resource.getFilename();
                    BarbershopFileParser.BarbershopMetadata metadata = parseFileName(filename);

                    TikaDocumentReader reader = new TikaDocumentReader(resource);
                    List<Document> documents = reader.read();
                    if(metadata != null){
                        for (Document doc : documents) {
                            doc.getMetadata().put("barbershop_name", metadata.barbershopName().toLowerCase());
                            doc.getMetadata().put("barbershop_city", metadata.city().toLowerCase());
                            doc.getMetadata().put("barbershop_category", metadata.category().toLowerCase());
                        }
                    }

                    List<Document> chunks = splitter.split(documents);
                    allChunks.addAll(chunks);
                }

                vectorStore.add(allChunks);
                System.out.println("Parsed " + allChunks.size() + " chunks into vector store.");

            } catch (Exception e) {
                System.err.println("Error loading documents: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

}
