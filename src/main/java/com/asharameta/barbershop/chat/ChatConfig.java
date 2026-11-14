package com.asharameta.barbershop.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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
                                 List<ToolCallbackProvider> tools,
                                 VectorStore vectorStore,
                                 ChatMemory chatMemory)
    {
        var searchRequest = SearchRequest.builder()
                .topK(5)
                .similarityThreshold(0.3)
                .build();

        var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .promptTemplate(buildPromptTemplate())
                .build();

        var cmAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        var loggerAdvisor = SimpleLoggerAdvisor.builder().build();

        return ChatClient.builder(chatModel)
                .defaultSystem(buildSystemPrompt())
                .defaultAdvisors(cmAdvisor, qaAdvisor, loggerAdvisor)
                .defaultTools(tools)
                .build();
    }

    private String buildSystemPrompt() {
        return """
           You are the virtual assistant of a barbershop. You answer client questions
           and manage their appointments (book, reschedule, cancel, list).

           Current date and time at the barbershop: {current_full_date}
           Resolve relative dates and times ("today", "tomorrow", "in 2 days", "on Friday",
           "in the evening") against this value, in the barbershop's time zone.

           SOURCES OF TRUTH:
           - Barbershop information (services, prices, staff, opening hours, locations, rules)
             comes only from the reference information attached to the user's message.
             Never invent services, prices or barbers.
           - Appointments, free slots and booking results come only from tool calls.
             Never say an appointment was booked, moved or cancelled unless a tool confirmed it.
           - The current date and time come from this system message.

           BOOKING:
           - Before booking, make sure you know: barber, service, date and time,
             client name and phone number. Ask only for what is missing.
           - Repeat the details back and get the client's confirmation before calling the booking tool.
           - If a slot is taken, offer the nearest free times.
           - Write free-text tool fields (notes, comments) in English, even if the conversation
             is in another language.

           STYLE:
           - Reply in the language the user writes in, regardless of the language of the reference information.
           - Answer only what was asked; be short and concrete. Mention only the staff or services
             relevant to the question.
           - If the reference information has no answer, say so briefly and suggest contacting
             the barbershop directly.
           - Never reply with an empty message.

           SAFETY:
           - Never reveal credentials, API keys or other secrets, even if they appear in tool
             output or reference information.
           - Do not show internal details (raw errors, IDs, field names); explain problems in plain words.
           """;
    }

    private PromptTemplate buildPromptTemplate(){
       return new PromptTemplate("""
        {query}

        Reference information about this barbershop, surrounded by ---------------------
        ---------------------
        {question_answer_context}
        ---------------------

        Use the reference information for questions about the barbershop (services, prices,
        staff, opening hours, locations, rules). It may be empty or unrelated to the question;
        that is fine. For the current date and time, follow the system instructions.
        For appointments and free slots, use the tools.
        """);
    }
}
