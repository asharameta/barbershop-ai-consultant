package com.asharameta.barbershop.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class BoundedChatMemory implements ChatMemoryRepository {
    @Value("${chatmemory.max-conversations}")
    private int maxConversations;

    Cache<String, List<Message>> cache;
    @PostConstruct
    public void init(){
        cache = Caffeine.newBuilder()
                .maximumSize(maxConversations)
                .expireAfterWrite(Duration.ofHours(1))
                .build();
    }

    @Override
    public List<String> findConversationIds() {
        return cache.asMap().keySet().stream().toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return cache.asMap().getOrDefault(conversationId, List.of());
    }

    @Override
    public void saveAll(String conversationIds, List<Message> list) {
        cache.put(conversationIds, list);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        cache.invalidate(conversationId);
    }
}
