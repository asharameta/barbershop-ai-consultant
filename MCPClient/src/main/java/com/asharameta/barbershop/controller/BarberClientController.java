package com.asharameta.barbershop.controller;

import com.asharameta.barbershop.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
public class BarberClientController {
    private static final Logger log = LoggerFactory.getLogger(BarberClientController.class);
    private final ChatClient chatClientMCP;

    public BarberClientController(ChatClient chatClientMCP) {
        this.chatClientMCP = chatClientMCP;
    }

    @PostMapping("/chat")
    public Answer chat(@RequestBody Question question) {
        log.info(String.valueOf(question));
        String filter = "barbershop_name == '%s' AND barbershop_city == '%s'".formatted(
                question.barbershopName().toLowerCase(Locale.ROOT),
                question.barbershopCity().toLowerCase(Locale.ROOT)
        );

        String response = chatClientMCP.prompt()
                .user(question.question())
                .advisors(a->a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filter))
                .call()
                .content();
        return new Answer(response);
    }
}
