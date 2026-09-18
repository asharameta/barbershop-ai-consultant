package com.asharameta.barbershop.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ChatConfig {
    @Bean
    ChatMemory chatMemory(BoundedChatMemory chatMemory){
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemory)
                .maxMessages(20)
                .build();
    }

    @Bean
    ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

    @Bean
    ChatClient chatClient(ChatModel chatModel,
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

        return ChatClient.builder(chatModel)
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
}
