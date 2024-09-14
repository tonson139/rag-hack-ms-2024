package com.leansixlab.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import org.postgresql.util.PGobject;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

class DocumentRowMapper implements RowMapper<Document> {

    private static final String COLUMN_EMBEDDING = "embedding";

    private static final String COLUMN_METADATA = "metadata";

    private static final String COLUMN_ID = "id";

    private static final String COLUMN_CONTENT = "content";

    private static final String COLUMN_DISTANCE = "distance";

    private ObjectMapper objectMapper;

    public DocumentRowMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Document mapRow(ResultSet rs, int rowNum) throws SQLException {
        String id = rs.getString(COLUMN_ID);
        String content = rs.getString(COLUMN_CONTENT);
        PGobject pgMetadata = rs.getObject(COLUMN_METADATA, PGobject.class);
        PGobject embedding = rs.getObject(COLUMN_EMBEDDING, PGobject.class);
        Float distance = rs.getFloat(COLUMN_DISTANCE);

        Map<String, Object> metadata = toMap(pgMetadata);
        metadata.put(COLUMN_DISTANCE, distance);

        Document document = new Document(id, content, metadata);
        document.setEmbedding(toFloatArray(embedding));

        return document;
    }

    private float[] toFloatArray(PGobject embedding) throws SQLException {
        return new PGvector(embedding.getValue()).toArray();
    }

    private Map<String, Object> toMap(PGobject pgObject) {

        String source = pgObject.getValue();
        try {
            return (Map<String, Object>) objectMapper.readValue(source, Map.class);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}