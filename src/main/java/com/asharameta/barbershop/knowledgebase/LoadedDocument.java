package com.asharameta.barbershop.knowledgebase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoadedDocument {
    private Long id;
    private String filename;
    private String contentHash;
    private String documentType;
    private Integer chunkCount;
    private OffsetDateTime loadedAt;
}
