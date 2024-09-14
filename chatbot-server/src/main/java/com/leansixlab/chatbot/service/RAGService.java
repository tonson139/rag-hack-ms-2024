package com.leansixlab.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);
    private final VectorStoreRepository vectorStoreRepository;
    private final ChatModel chatModel;
    private final OllamaOptions options;
    private final ObjectMapper objectMapper;

    @Autowired
    public RAGService(VectorStoreRepository vectorStoreRepository, ChatModel chatModel, OllamaOptions options, ObjectMapper objectMapper) {
        this.vectorStoreRepository = vectorStoreRepository;
        this.chatModel = chatModel;
        this.options = options;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    public String generateResponse(String userPrompt) {
        var searchResult = this.vectorStoreRepository.doSimilaritySearch(SearchRequest.query(userPrompt));
        log.info("searchResult: id={}", objectMapper.writeValueAsString(searchResult.stream().map(it -> it.getId())));
        var bookTitleLists = searchResult.stream().map(it -> (String) it.getMetadata().get("Title")).collect(Collectors.joining(","));
        var templatePrompt = String.format("You as a Political science Teaching Assistant. Answer student question: %s. and reference only this books in the list to answer books list is [%s]. If don't have answer 'you don't know'",
                userPrompt,
                bookTitleLists);
        log.info("templatePrompt: {}", templatePrompt);
        var response = chatModel.call(new Prompt(templatePrompt, options));
        return response.getResult().getOutput().getContent();
    }
}
