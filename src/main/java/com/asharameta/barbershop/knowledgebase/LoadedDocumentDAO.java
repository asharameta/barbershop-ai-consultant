package com.asharameta.barbershop.knowledgebase;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository
public class LoadedDocumentDAO {
    private final JdbcTemplate jdbc;

    boolean existByFilenameAndContentHash(String filename, String contentHash){
        String sql = "SELECT EXISTS(SELECT 1 FROM loaded_document WHERE filename = ? AND content_hash = ?)";
        return Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, filename, contentHash));
    }

    void loadDocument(LoadedDocument loadedDocument){
        jdbc.update("INSERT INTO loaded_document (filename, content_hash, document_type, chunk_count) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",
                loadedDocument.getFilename(), loadedDocument.getContentHash(), loadedDocument.getDocumentType(), loadedDocument.getChunkCount());

    }
}
