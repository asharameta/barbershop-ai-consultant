package com.asharameta.barbershop.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    @Mock
    ChatModel chatModel;

    ChatClient chatClient;
    ChatService chatService;

    @Captor
    ArgumentCaptor<Prompt> promptCaptor;


    @BeforeEach
    public void init(){
        chatClient = ChatClient.builder(chatModel)
                .defaultSystem("Now: {current_full_date}").build();
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
    }

    @Test
    void putsCurrentDateIntoSystemPrompt(){
        Clock fixed = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), WARSAW);

        String systemPrompt = systemPromptAt(fixed);

        assertTrue(systemPrompt.contains("Thursday"));
        assertTrue(systemPrompt.contains("November 12, 2026"));
        assertFalse(systemPrompt.contains("{current_full_date}"));
    }

    @Test
    void usesBarbershopTimeZone(){
        Clock fixed = Clock.fixed(Instant.parse("2026-11-12T23:00:00Z"), WARSAW);

        String systemPrompt = systemPromptAt(fixed);

        assertFalse(systemPrompt.contains("Thursday"));
        assertTrue(systemPrompt.contains("Friday"));
        assertFalse(systemPrompt.contains("November 12"));
        assertTrue(systemPrompt.contains("November 13, 2026"));
        assertFalse(systemPrompt.contains("23:00"));
        assertTrue(systemPrompt.contains("00:00"));
    }

    @Test
    void computesTimeOnEveryRequest(){
        Clock fixed = mock(Clock.class);

        when(fixed.instant()).thenReturn(Instant.parse("2026-11-12T13:30:00Z"), Instant.parse("2026-12-23T11:10:00Z"));
        when(fixed.getZone()).thenReturn(WARSAW);
        when(chatModel.call(any(Prompt.class))).thenReturn(ChatResponse.builder().generations(List.of(new Generation(AssistantMessage.builder().content("ok").build()))).build());
        chatService = new ChatService(chatClient, fixed);

        chatService.ask("hi", "1a-ba", 1L, "");
        chatService.ask("hi2", "1a-bb", 2L, "");

        verify(chatModel, times(2)).call(promptCaptor.capture());

        var systemPrompts = promptCaptor.getAllValues();

        String firstPrompt = systemPrompts.getFirst().getSystemMessage().getText();
        String secondPrompt = systemPrompts.get(1).getSystemMessage().getText();

        assertNotEquals(firstPrompt, secondPrompt);

        assertTrue(firstPrompt.contains("Thursday"));
        assertTrue(firstPrompt.contains("November 12, 2026"));
        assertTrue(firstPrompt.contains("14:30"));

        assertTrue(secondPrompt.contains("Wednesday"));
        assertTrue(secondPrompt.contains("December 23, 2026"));
        assertTrue(secondPrompt.contains("12:10"));

    }

    String systemPromptAt(Clock clock){
        when(chatModel.call(any(Prompt.class))).thenReturn(ChatResponse.builder().generations(List.of(new Generation(AssistantMessage.builder().content("ok").build()))).build());
        chatService = new ChatService(chatClient, clock);

        chatService.ask("hi", "1a-ba", 1L, "");

        verify(chatModel).call(promptCaptor.capture());

        return promptCaptor.getValue().getSystemMessage().getText();
    }

}