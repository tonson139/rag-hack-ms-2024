package com.leansixlab.chatbot.controller;

import com.leansixlab.chatbot.model.MessageRequest;
import com.leansixlab.chatbot.model.Response;
import com.leansixlab.chatbot.service.ArrayUtil;
import com.leansixlab.chatbot.service.RAGService;
import com.leansixlab.chatbot.service.VectorStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final EmbeddingModel embeddingModel;
    private final VectorStoreRepository vectorStore;
    private final ChatModel chatModel;
    private final OllamaOptions options;
    private final RAGService service;

    @Autowired
    public ChatController(EmbeddingModel embeddingModel, VectorStoreRepository vectorStore, ChatModel chatModel, OllamaOptions options, RAGService service) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.options = options;
        this.service = service;
    }

    @PostMapping("/ai/embedding")
    public Response<float[]> embed(@RequestBody MessageRequest request) {
        final var embedded = embeddingModel.embed(request.getMessage());

        return new Response<float[]>()
                .setStatus("OK")
                .setResult(ArrayUtil.concatenateArrays(embedded, embedded));
    }

    @PostMapping("/ai/search")
    public Response<List<Document>> search(@RequestBody MessageRequest request) {
        var searchRequest = SearchRequest.query(request.getMessage());
        final var result = vectorStore.doSimilaritySearch(searchRequest);

        return new Response<List<Document>>()
                .setStatus("OK")
                .setResult(result);
    }

    @PostMapping("/ai/ollama/chat")
    public Response<String> chatWithOllama(@RequestBody MessageRequest request) {
        ChatResponse response = chatModel.call(new Prompt(request.getMessage(), options));
        return new Response<String>()
                .setStatus("OK")
                .setResult(response.getResult().getOutput().getContent());
    }

    @PostMapping("/ai/generate-response")
    public String getRAG(@RequestBody MessageRequest request) {
        return service.generateResponse(request.getMessage());
    }

    @PostMapping("/mock/ai/generate-response")
    public String mockRAG(@RequestBody MessageRequest request) {
        return "Hey this is mock response";
    }

}