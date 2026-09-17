package com.asharameta.barbershop.config.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiProviderConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ai.chat", name = "provider", havingValue = "openai", matchIfMissing = false)
    ChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${ai.chat.model}") String model,
            @Value("${ai.chat.temperature}") double temperature
    ){
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .apiKey(apiKey)
                        .model(model)
                        .temperature(temperature)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ai.embedding", name = "provider", havingValue = "openai", matchIfMissing = false)
    EmbeddingModel openAiEmbeddingModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${ai.embedding.model}") String model
    ){
        return OpenAiEmbeddingModel.builder()
                .metadataMode(MetadataMode.EMBED)
                .options(OpenAiEmbeddingOptions.builder()
                        .apiKey(apiKey)
                        .model(model)
                        .build())
                .build();

    }
}
