package com.barbershopAIConsultant.Controller;

import com.barbershopAIConsultant.Model.Answer;
import com.barbershopAIConsultant.Model.Question;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BarberClientController {
    private final ChatClient chatClientMCP;
    private final VectorStore vectorStore;

    public BarberClientController(ChatClient chatClientMCP, VectorStore vectorStore) {
        this.chatClientMCP = chatClientMCP;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/search")
    private void searchTest(){
        SearchRequest request = SearchRequest.builder()
                .query("any")
                        .topK(999)
                                .filterExpression("barbershop_name == 'anchor'").build();

        List<Document> results = vectorStore.similaritySearch(request);

        System.out.println("Pre-filter matched: " + results.size());
        results.forEach(doc ->
                System.out.println(doc.getMetadata())
        );
    }

    @PostMapping("/chat")
    public Answer chat(@RequestBody Question question) {
        String response = chatClientMCP.prompt()
                .user(question.question())
                .call()
                .content();
        return new Answer(response);
    }
}
