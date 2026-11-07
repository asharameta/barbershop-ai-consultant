package com.asharameta.barbershop.knowledgebase;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@AllArgsConstructor
@Component
class KnowledgeBaseIngestor {
    private final VectorStore vectorStore;
    private final LoadedDocumentDAO loadedDocumentDAO;
    private final KnowledgeBaseLoader loader;

    void ingest(){
        List<KnowledgeBaseData> resources = loader.getResources();

        resources.stream()
                .filter(resource -> {
                    if(resource.metadata() == null){
                        log.warn("file {} has null metadata", resource.filename());
                        return false;
                    }
                    return !loadedDocumentDAO.existByFilenameAndContentHash(resource.filename(), resource.resourceContentHash());
                })
                .forEach(resource ->{
                    List<Document> chunks = loader.getChunks(resource);
                    vectorStore.accept(chunks);

                    String filename = resource.filename();
                    FileExtension fileExtension = FileExtension.fromFileName(filename).orElseThrow();
                    LoadedDocument loadedDocument = LoadedDocument.builder()
                            .filename(filename)
                            .documentType(fileExtension.getExtension())
                            .contentHash(resource.resourceContentHash())
                            .chunkCount(chunks.size())
                            .build();

                    loadedDocumentDAO.loadDocument(loadedDocument);
                });
    }

}
