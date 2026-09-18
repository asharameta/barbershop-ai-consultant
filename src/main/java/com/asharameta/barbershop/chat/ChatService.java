package com.asharameta.barbershop.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
class ChatService {
    private final ChatClient chatClient;

    ChatService(ChatClient chatClient){
        this.chatClient = chatClient;
    }

    String ask(String question, String conversationId, String filter){
        return chatClient.prompt()
                .user(question)
                .advisors(a->a
                        .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filter)
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
