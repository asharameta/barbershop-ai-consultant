package com.asharameta.barbershop.chat;

import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

@Service
class ChatService {
    private final Clock clock;
    private final ChatClient chatClient;

    ChatService(ChatClient chatClient, Clock clock){
        this.chatClient = chatClient;
        this.clock = clock;
    }


    String ask(String question, String conversationId, long barbershopId, String filter){
        return chatClient.prompt()
                .system(systemSpecConsumer())
                .user(question)
                .advisors(advisorSpecConsumer(conversationId, filter))
                .toolContext(toolContext(barbershopId))
                .call()
                .content();
    }


    SseEmitter askWithStreaming(String question, String conversationId, long barbershopId, String filter){
        SseEmitter sseEmitter = new SseEmitter(0L);
        StringBuilder answer = new StringBuilder();

        chatClient.prompt()
                .system(systemSpecConsumer())
                .user(question)
                .advisors(advisorSpecConsumer(conversationId, filter))
                .toolContext(toolContext(barbershopId))
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

    private Consumer<ChatClient.PromptSystemSpec> systemSpecConsumer(){
        ZonedDateTime zonedDateTimeNow = ZonedDateTime.now(clock);

        var formattedDate = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(Locale.ENGLISH)
                .format(zonedDateTimeNow);

        return (sysSpec) -> sysSpec.params(
            Map.of("current_full_date", formattedDate)
        );
    }

    private Consumer<ChatClient.AdvisorSpec> advisorSpecConsumer(String conversationId, String filter){
        return (advisorSpec)->advisorSpec
                .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filter)
                .param(ChatMemory.CONVERSATION_ID, conversationId);
    }

    private Map<String, Object> toolContext(long barbershopId){
        return Map.of("barbershopId", barbershopId);
    }
}
