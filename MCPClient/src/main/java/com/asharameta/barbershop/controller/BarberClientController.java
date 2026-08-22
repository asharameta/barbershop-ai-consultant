package com.asharameta.barbershop.controller;

import com.asharameta.barbershop.model.Answer;
import com.asharameta.barbershop.model.Question;
import com.asharameta.barbershop.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
public class BarberClientController {
    private final AiChatService chatService;

    public BarberClientController(AiChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public Answer chat(@Valid @RequestBody Question question) {
        String filter = "barbershop_name == '%s' AND barbershop_city == '%s'".formatted(
                question.barbershopName().toLowerCase(Locale.ROOT),
                question.barbershopCity().toLowerCase(Locale.ROOT)
        );
        return new Answer(chatService.ask(question.question(), question.conversationId(), filter));
    }
}
