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

    private final String PROMPT_INTENT_VALIDATE = """
            Is this question asking for a Political science <question>%s<question>. Answer only 'YES' or 'NO'"
            """;
    private final String PROMPT_KEYWORD_EXTRACT = """
            What is the keywords or meaning about Political science in this question asking <question>%s<question>.
            Strictly Answer in this format <format>['keyword_1','keyword_2',...'keyword_n']<format>
            """;
    private final String PROMPT_GENERATE_ANSWER = """
            "You as a Political science Teaching Assistant. Answer student question: <question>%s<question>.
            Reference your answer only this books in the list to answer books list is [%s]. If don't have answer 'you don't know'";
            """;

    @SneakyThrows
    public String generateResponse(String userPrompt) {
        var responseValidateIntent = chatModel.call(new Prompt(String.format(PROMPT_INTENT_VALIDATE, userPrompt), options));
        var isAboutBook = responseValidateIntent.getResult().getOutput().getContent();
        log.info("pre-defined intent isAboutBook: {}", isAboutBook);

        if ("NO".equals(isAboutBook))
            return "I'm Political science Teaching Assistant. I can only answer about book suggestions";

        var responseKeyword = chatModel.call(new Prompt(String.format(PROMPT_KEYWORD_EXTRACT, userPrompt), options));
        var searchKeyword = responseKeyword.getResult().getOutput().getContent();
        log.info("keyword extracting: {}", searchKeyword);

        var searchResult = this.vectorStoreRepository.doSimilaritySearch(SearchRequest.query(searchKeyword));
        log.info("searchResult: id={}", objectMapper.writeValueAsString(searchResult.stream().map(it -> it.getId())));
        var bookTitleLists = searchResult.stream().map(it -> (String) it.getMetadata().get("Title")).collect(Collectors.joining(","));
        var templatePrompt = String.format(PROMPT_GENERATE_ANSWER, userPrompt, bookTitleLists);
        log.info("templatePrompt: {}", templatePrompt);
        var response = chatModel.call(new Prompt(templatePrompt, options));
        return response.getResult().getOutput().getContent();
    }
}
