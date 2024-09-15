package com.leansixlab.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
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
            You as a music recommender. you can only recommend music that match the user preference. Is this user question related to music recommending.
            <user-question>
            %s
            <user-question>
            Strictly answer only 'YES' or 'NO'
            """;
    private final String PROMPT_KEYWORD_EXTRACT = """
            You as a music recommender. What is the keywords about music preference form this user question.
            <user-question>%s<user-question>.
            Strictly answer in this json format '{ keywords: string[] }'
            """;

    private final String PROMPT_CHOOSING_BEST_FIT = """
            You as a music recommender. Which one of this song it more related to user question: %s.
            Song A
                metadata: %s
                lyrics: %s
            Song B
                metadata: %s
                lyrics: %s
            Answer only 'A' or 'B'
            """;
    private final String PROMPT_SUMMARY = """
            Summarize this random word: %s.
            
            Do not write "Here's a summary", only write the content.
            """;
    private final String PROMPT_GENERATE_ANSWER_WITH_REFERENCE = """
            New conversation.
            You as a music recommender. This is the information form the music database. 
            This is metadata of the song: %s, 
                        
            This is the summary of the song %s.
                        
            Answer this user question that you recommend this song with information above: %s
            """;
    private final String PROMPT_GENERATE_ANSWER_NO_REFERENCE = """
            You as a music recommender. Strictly answer to this user question that your don't have any recommend form for this question.
            
            <user-question>
            %s
            <user-question>
            
            Do not write "Based on context you provided", only write the content.
            """;

    @SneakyThrows
    public String generateResponse(String userPrompt) {
        var validateIntentPrompt = new Prompt(String.format(PROMPT_INTENT_VALIDATE, userPrompt), options);
        var responseValidateIntent = chatModel.call(validateIntentPrompt);
        var isRelated = responseValidateIntent.getResult().getOutput().getContent();
        log.info("[generateResponse][1]\n pre-defined intent isRelated prompt: {}, answer={}", validateIntentPrompt.getContents(), isRelated);

        if ("NO".equals(isRelated))
            return "I'm music recommender. I can recommending music based on your provided preference.";

        var keywordExtractPrompt = new Prompt(String.format(PROMPT_KEYWORD_EXTRACT, userPrompt), options.copy().withFormat("json"));
        var responseKeyword = chatModel.call(keywordExtractPrompt);
        var searchKeyword = responseKeyword.getResult().getOutput().getContent();
        log.info("[generateResponse][2]\n keyword extracting prompt: {}, answer: {}", keywordExtractPrompt, searchKeyword);

        var searchResult = this.vectorStoreRepository.doSimilaritySearch(SearchRequest
                .query(userPrompt)
                .withSimilarityThreshold(0.3)
                .withTopK(3));
        var searchResultLists = searchResult.stream().map(it -> it.getMetadata().toString()).collect(Collectors.joining(","));
        log.info("searchResultLists {}", searchResultLists);

        if (searchResult.isEmpty()) {
            var templatePrompt = new Prompt(String.format(PROMPT_GENERATE_ANSWER_NO_REFERENCE, userPrompt), options);
            var response = chatModel.call(templatePrompt);
            log.info("[generateResponse][2.1]\n Not found reference. templatePrompt: {}, answer: {}", templatePrompt.getContents(), response.getResult().getOutput().getContent());
            return response.getResult().getOutput().getContent();
        }

        var bestFitPrompt = new Prompt(String.format(PROMPT_CHOOSING_BEST_FIT, userPrompt,
                searchResult.get(0).getMetadata(),
                searchResult.get(0).getContent(),
                searchResult.get(1).getMetadata(),
                searchResult.get(1).getContent())
        );
        var responseBestFit = chatModel.call(bestFitPrompt);
        log.info("[bestFit] prompt {} ,answer {}", bestFitPrompt, responseBestFit.getResult().getOutput().getContent());
        Document bestFitResult;
        if ("A".equals(responseBestFit.getResult().getOutput().getContent())) {
            bestFitResult = searchResult.getFirst();
        } else bestFitResult = searchResult.getLast();


        var summarySongPrompt = new Prompt(String.format(PROMPT_SUMMARY, bestFitResult.getContent()));
        var responseSummary = chatModel.call(summarySongPrompt).getResult().getOutput().getContent();
        log.info("[summary] {}, {}", summarySongPrompt, responseSummary);


        log.info("[generateResponse][2.2]\n searchResult: id={}", objectMapper.writeValueAsString(searchResult.stream().map(it -> it.getId())));
        var templatePrompt = new Prompt(String.format(PROMPT_GENERATE_ANSWER_WITH_REFERENCE, bestFitResult.getMetadata(), responseSummary, userPrompt), options);
        var response = chatModel.call(templatePrompt);
        log.info("[generateResponse][3]\n \n\n============ templatePrompt\n {}, \n\n============ answer\n {}", templatePrompt.getContents(), response.getResult().getOutput().getContent());
        return response.getResult().getOutput().getContent();
    }
}
