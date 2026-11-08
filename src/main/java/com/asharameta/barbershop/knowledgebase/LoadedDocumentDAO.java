package com.asharameta.barbershop.knowledgebase;


import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@AllArgsConstructor
@Repository
public class LoadedDocumentDAO {
    private final JdbcTemplate jdbc;

    Optional<String> getHashByFilename(String filename){
        String sql = "SELECT content_hash FROM loaded_document WHERE filename = ?";
        return jdbc.query(sql, new SingleColumnRowMapper<>(String.class), filename).stream().findFirst();
    }

    List<String> getAllFilenames(){
        String sql = "SELECT filename FROM loaded_document";
        return jdbc.queryForList(sql, String.class);
    }

    void deleteDocumentByName(String filename){
        String sql = "DELETE FROM loaded_document WHERE filename = ?";

        jdbc.update(sql, filename);
    }

    void loadDocument(LoadedDocument loadedDocument){
        String sql = """
                         INSERT INTO loaded_document (filename, content_hash, document_type, chunk_count) VALUES (?,?,?,?)
                         ON CONFLICT (filename) DO UPDATE
                         SET content_hash = EXCLUDED.content_hash, chunk_count = EXCLUDED.chunk_count, loaded_at = EXCLUDED.loaded_at;
                         """;
        jdbc.update(sql,
                loadedDocument.getFilename(), loadedDocument.getContentHash(), loadedDocument.getDocumentType(), loadedDocument.getChunkCount());

    }
}
