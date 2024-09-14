package com.leansixlab.chatbot.service;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class Configs {

    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    @Value("${spring.ai.ollama.chat.options.model}")
    private String model;

    @Bean
    RestClient.Builder getRestClient() {
        var http = new HttpComponentsClientHttpRequestFactory();
        http.setConnectionRequestTimeout(Duration.ofMinutes(7));
        return RestClient.builder()
                .requestFactory(http)
                .defaultHeader("ngrok-skip-browser-warning", "true")
                .defaultHeader("Authorization", "Basic YWRtaW46YWRtaW4xMjM0IQ==");
    }

    @Bean
    OllamaApi getOllamaApi(RestClient.Builder builder) {
        return new OllamaApi(this.baseUrl, builder);
    }

    @Bean
    OllamaOptions getOllamaOptions() {
        return OllamaOptions.builder()
                .withModel(model)
                .withNumCtx(100)
                .withTemperature(0.2f)
                .build();
    }
}
