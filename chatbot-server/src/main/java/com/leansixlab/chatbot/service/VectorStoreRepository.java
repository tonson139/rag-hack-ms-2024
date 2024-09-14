package com.leansixlab.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VectorStoreRepository {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.ai.vectorstore.pgvector.schema-name}")
    private String schemaName;

    @Value("${spring.ai.vectorstore.pgvector.table-name}")
    private String vectorTableName;

    private ObjectMapper objectMapper = new ObjectMapper();


    private String getFullyQualifiedTableName() {
        return this.schemaName + "." + this.vectorTableName;
    }

    public List<Document> doSimilaritySearch(SearchRequest request) {

        String jsonPathFilter = "";

        double distance = 1 - request.getSimilarityThreshold();

        PGvector queryEmbedding = getQueryEmbedding(request.getQuery());

        return this.jdbcTemplate.query(
                String.format("SELECT *, embedding <=> ? AS distance FROM %s WHERE embedding <=> ? < ? %s ORDER BY distance LIMIT ? ", getFullyQualifiedTableName(),
                        jsonPathFilter),
                new DocumentRowMapper(this.objectMapper), queryEmbedding, queryEmbedding, distance, request.getTopK());
    }

    private PGvector getQueryEmbedding(String query) {
        float[] embedding = this.embeddingModel.embed(query);
        return new PGvector(ArrayUtil.concatenateArrays(embedding, embedding));
    }
}
