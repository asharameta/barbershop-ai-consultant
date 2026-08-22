package com.asharameta.barbershop.controller;

import com.asharameta.barbershop.model.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.filter.converter.PrintFilterExpressionConverter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
public class BarberClientController {
    private static final Logger log = LoggerFactory.getLogger(BarberClientController.class);
    private final ChatClient chatClientMCP;

    public BarberClientController(ChatClient chatClientMCP) {
        this.chatClientMCP = chatClientMCP;
    }

    @PostMapping("/chat")
    public Answer chat(@Valid @RequestBody Question question) {
        var b = new FilterExpressionBuilder();

        var bName = b.eq("barbershop_name", question.barbershopName().toLowerCase(Locale.ROOT));
        var bCity = b.eq("barbershop_city", question.barbershopCity().toLowerCase(Locale.ROOT));

        var filter = new PrintFilterExpressionConverter().convertExpression(b.and(bName, bCity).build());

        String response = chatClientMCP.prompt()
                .user(question.question())
                .advisors(a->a
                        .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filter)
                        .param(ChatMemory.CONVERSATION_ID, question.conversationId()))
                .call()
                .content();
        return new Answer(response);
    }
}
