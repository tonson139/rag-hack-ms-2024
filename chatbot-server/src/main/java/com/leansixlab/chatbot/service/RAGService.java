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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private final String PROMPT_SUMMARY = """
            Summarize this random word: %s.
            Do not write "Here's a summary", only write the content.
            """;
    private final String SONG_INFORMATION = """
                        
            This is the meta data information from the song '%s': %s
                        
            This is the lyrics of song '%s': %s
                        
            """;
    private final String PROMPT_IS_METADATA_FOUND = """
            This is the user question: %s
            Can you find the answer of this question form this metadata
            
            %s
            
            %s
            
            Strictly answer 'YES' or 'NO'
            """;
    private final String PROMPT_USING_METADATA_TO_ANSWER = """
            This is the user question: %s
            Using one of this metadata to answer user question. Do not use other information to answer. If not enough information then answer "You don't know".
            
            %s
            
            %s
            
            """;
    private final String PROMPT_GENERATE_ANSWER_WITH_REFERENCE = """
            This is the user question: %s
                        
            %s
                        
            %s
                        
            Answer the user question by selecting either information from '%s' or '%s' that are more closely relate to the user question. Why you select this song?
            If you think that both song are not related to user question. You must say that there is no song in the database that match the user description.
            
            Do not make up information. 
            Do not mention the another song in the output.
            Do not use the sentence that means 'Based on the user's question' when answer. 
            Do not mention the word that means summary.
            """;
    private final String PROMPT_GENERATE_ANSWER_WITH_REFERENCE_2 = """
            This is the user question: %s
                        
            %s
                        
            %s
                        
            Answer the user question by selecting either information from '%s' or '%s' that are more closely relate to the user question and why you choose this answer.
            If you think that both song are not related to user question. You must say that there is no song in the database that match the user description.
            
            Do not make up information. 
            Do not mention the another song in the output.
            Do not use the sentence that means 'Based on the user's question' when answer. 
            Do not mention the word that means summary.
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
        var searchResult = this.vectorStoreRepository.doSimilaritySearch(SearchRequest
                .query(userPrompt)
//                .withSimilarityThreshold(0.3)
                .withTopK(2));
        var searchResultLists = searchResult.stream().map(it -> it.getMetadata().toString()).collect(Collectors.joining(",\n"));
        log.info("searchResultLists \n{}", searchResultLists);

        if (searchResult.isEmpty()) {
            var templatePrompt = new Prompt(String.format(PROMPT_GENERATE_ANSWER_NO_REFERENCE, userPrompt), options);
            var response = chatModel.call(templatePrompt);
            return response.getResult().getOutput().getContent();
        }


        List<String> songList = new ArrayList<>();
        for (var candidate : searchResult) {
            var summarySongPrompt = new Prompt(String.format(PROMPT_SUMMARY, candidate.getContent()));
            var responseSummary = chatModel.call(summarySongPrompt).getResult().getOutput().getContent();
            log.info("[summary] {}, {}", summarySongPrompt, responseSummary);
            songList.add(String.format(SONG_INFORMATION,
                            candidate.getMetadata().get("track_name"),
                            candidate.getContent(),
                            candidate.getMetadata().get("track_name"),
                            responseSummary
                    )
            );
        }

        var findMetaDatePrompt = new Prompt(String.format(PROMPT_IS_METADATA_FOUND,
                userPrompt,
                searchResult.get(0).getMetadata(),
                searchResult.get(1).getMetadata()));
        var isFound = chatModel.call(findMetaDatePrompt).getResult().getOutput().getContent();
        log.info("isFound {} {}", findMetaDatePrompt.getContents(), isFound);
        if("YES".equals(isFound)) {
            var answerFromMetaData = new Prompt(String.format(PROMPT_USING_METADATA_TO_ANSWER,
                    userPrompt,
                    searchResult.get(0).getMetadata(),
                    searchResult.get(1).getMetadata()));
            log.info("YES {} {}", findMetaDatePrompt.getContents(), answerFromMetaData);
            return chatModel.call(answerFromMetaData).getResult().getOutput().getContent();
        }

        var templatePrompt = new Prompt(String.format(
                    PROMPT_GENERATE_ANSWER_WITH_REFERENCE,
                    userPrompt,
                    songList.get(0),
                    songList.get(1),
                    searchResult.get(0).getMetadata().get("track_name"),
                    searchResult.get(1).getMetadata().get("track_name")),
                options);
        var response = chatModel.call(templatePrompt);
        log.info("[generateResponse][3]\n \n\n============ templatePrompt\n {}, \n\n============ answer\n {}", templatePrompt.getContents(), response.getResult().getOutput().getContent());
        return response.getResult().getOutput().getContent();
    }
}
