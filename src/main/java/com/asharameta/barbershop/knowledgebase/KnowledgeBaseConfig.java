package com.asharameta.barbershop.knowledgebase;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
public class KnowledgeBaseConfig {
    @Value("${ai.embedding.dimensions}")
    private int dimensions;

    @Value("${asharameta.barbershop.knowledge-base.resource-pattern}")
    String resourcePattern;

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel){
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .initializeSchema(true)
                .dimensions(dimensions)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .build();
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
            List<Document> documents = knowledgeBaseLoader.loadDocuments();
            vectorStore.accept(documents);
        };
    }
}
