package com.asharameta.barbershop.chat;

import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Service
class ChatService {
    private final ChatClient chatClient;

    ChatService(ChatClient chatClient){
        this.chatClient = chatClient;
    }

    String ask(String question, String conversationId, long barbershopId, String filter){
        return chatClient.prompt()
                .user(question)
                .advisors(advisorSpec->advisorSpec
                        .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filter)
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of("barbershopId", barbershopId))
                .call()
                .content();
    }


    SseEmitter askWithStreaming(String question, String conversationId, long barbershopId, String filter){
        SseEmitter sseEmitter = new SseEmitter(0L);

        StringBuilder answer = new StringBuilder();

        chatClient.prompt()
                .user(question)
                .advisors(a->a
                        .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filter)
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of("barbershopId", barbershopId))
                .stream()
                .chatResponse()
                .subscribe(
                        (ChatResponse response)-> processToken(response, sseEmitter, answer),
                        sseEmitter::completeWithError,
                        sseEmitter::complete
                );

        return sseEmitter;
    }

    @SneakyThrows
    private static void processToken(ChatResponse response, SseEmitter emitter, StringBuilder sb){
        var token = response.getResult().getOutput();
        emitter.send(token);
        sb.append(token.getText());
    }
}
