package com.asharameta.barbershop.controller;

import com.asharameta.barbershop.model.Answer;
import com.asharameta.barbershop.model.IdempotencyDTO;
import com.asharameta.barbershop.model.Question;
import com.asharameta.barbershop.service.AiChatService;
import com.asharameta.barbershop.service.IdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@Validated
@RestController
@RequestMapping("/api/v1/")
public class BarberClientController {
    private final IdempotencyService idempotencyService;
    private final AiChatService chatService;
    private final ObjectMapper objectMapper;

    public BarberClientController(AiChatService chatService, IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/chat")
    public ResponseEntity<Answer> chat(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9-_]{1,128}$") @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody Question question) throws JsonProcessingException {

        IdempotencyDTO claimed = idempotencyService.claim(question.toString(), idempotencyKey);

        return switch (claimed.state()){
            case LOCK_EXPIRED, KEY_REUSED, DUPLICATE_IN_PROGRESS ->
                    ResponseEntity.status(claimed.responseStatus()).body(new Answer(claimed.responseBody()));

            case COMPLETED -> ResponseEntity.status(HttpStatus.OK)
                    .body(objectMapper.readValue(claimed.responseBody(), Answer.class));

            case CLAIMED -> {
                String filter = "barbershop_name == '%s' AND barbershop_city == '%s'".formatted(
                        question.barbershopName().toLowerCase(Locale.ROOT),
                        question.barbershopCity().toLowerCase(Locale.ROOT)
                );

                try{
                    Answer answer = new Answer(chatService.ask(question.question(), question.conversationId(), filter));
                    idempotencyService.complete(idempotencyKey, question.toString(), objectMapper.writeValueAsString(answer));
                    yield ResponseEntity.status(HttpStatus.OK).body(answer);
                }catch (RuntimeException e){
                    idempotencyService.release(idempotencyKey);
                    throw e;
                }
            }
        };
    }
}
