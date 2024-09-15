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
            You as a Philosophy and Political science Teaching Assistant. Is this student question question asking about a Philosophy and Political science. 
            <student-question>%s<student-question>.
            Strictly answer only 'YES' or 'NO'
            """;
    private final String PROMPT_KEYWORD_EXTRACT = """
            What is the keywords or meaning about Philosophy and Political science in this question asking.
            <question>%s<question>.
            Strictly answer in this json format '{ keywords: string[] }'
            """;
    private final String PROMPT_GENERATE_ANSWER_WITH_REFERENCE = """
            You as a Philosophy and Political science Teaching Assistant. Answer student question:
            <question>%s<question>.
            Reference your answer with the books in this list [%s] as a bullet point at the ending of answer.
            """;
    private final String PROMPT_GENERATE_ANSWER_NO_REFERENCE = """
            You as a Philosophy and Political science Teaching Assistant. Do not hallucinate!. Answer student question:
            <question>%s<question>.
            Also tell that you don't have a reference book for this question.
            """;

    @SneakyThrows
    public String generateResponse(String userPrompt) {
        var validateIntentPrompt = new Prompt(String.format(PROMPT_INTENT_VALIDATE, userPrompt), options);
        var responseValidateIntent = chatModel.call(validateIntentPrompt);
        var isRelated = responseValidateIntent.getResult().getOutput().getContent();
        log.info("[generateResponse][1]\n pre-defined intent isRelated prompt: {}, answer={}", validateIntentPrompt.getContents(), isRelated);

        if ("NO".equals(isRelated))
            return "I'm Philosophy and Political science Teaching Assistant. I can only answer about Philosophy and Political science.";

        var keywordExtractPrompt = new Prompt(String.format(PROMPT_KEYWORD_EXTRACT, userPrompt), options.copy().withFormat("json"));
        var responseKeyword = chatModel.call(keywordExtractPrompt);
        var searchKeyword = responseKeyword.getResult().getOutput().getContent();
        log.info("[generateResponse][2]\n keyword extracting prompt: {}, answer: {}", keywordExtractPrompt, searchKeyword);

        var searchResult = this.vectorStoreRepository.doSimilaritySearch(SearchRequest
                .query(searchKeyword)
                .withSimilarityThreshold(0.65)
                .withTopK(2));

        if(searchResult.isEmpty()) {
            var templatePrompt = new Prompt(String.format(PROMPT_GENERATE_ANSWER_NO_REFERENCE, userPrompt), options);
            var response = chatModel.call(templatePrompt);
            log.info("[generateResponse][2.1]\n Not found reference. templatePrompt: {}, answer: {}", templatePrompt.getContents(), response.getResult().getOutput().getContent());
            return response.getResult().getOutput().getContent();
        }

        log.info("[generateResponse][2.2]\n searchResult: id={}", objectMapper.writeValueAsString(searchResult.stream().map(it -> it.getId())));
        var bookTitleLists = searchResult.stream().map(it -> (String) it.getMetadata().get("Title")).collect(Collectors.joining(","));
        var templatePrompt = new Prompt(String.format(PROMPT_GENERATE_ANSWER_WITH_REFERENCE, userPrompt, bookTitleLists), options);
        var response = chatModel.call(templatePrompt);
        log.info("[generateResponse][3]\n templatePrompt: {}, answer: {}", templatePrompt.getContents(), response.getResult().getOutput().getContent());
        return response.getResult().getOutput().getContent();
    }
}
