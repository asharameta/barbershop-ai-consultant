package com.asharameta.barbershop.config;

import com.asharameta.barbershop.knowledgebase.KnowledgeBaseLoader;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.List;

@Configuration
public class IngestionConfig {
    @Value("${asharameta.barbershop.knowledge-base.resource-pattern}")
    String resourcePattern;

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
