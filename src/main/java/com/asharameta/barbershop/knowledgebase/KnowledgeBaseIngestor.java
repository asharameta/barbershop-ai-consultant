package com.asharameta.barbershop.knowledgebase;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@AllArgsConstructor
@Component
class KnowledgeBaseIngestor {
    private final VectorStore vectorStore;
    private final LoadedDocumentDAO loadedDocumentDAO;
    private final KnowledgeBaseLoader loader;

    void ingest(){
        List<KnowledgeBaseData> resources = loader.getResources();

        Set<String> filenamesFromDisk = new HashSet<>();

        resources.stream()
                .filter( resource ->{
                    filenamesFromDisk.add(resource.filename());
                    return filterResource(resource);
                 })
                .forEach(resource ->{
                    deleteFromVectorStoreByName(resource.filename());

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
                    filenamesFromDisk.add(filename);
                });

        deleteDocuments(filenamesFromDisk);
    }

    private boolean filterResource(KnowledgeBaseData resource){
        if(resource.metadata() == null){
            log.warn("file {} has null metadata", resource.filename());
            return false;
        }

        Optional<String> dbHash = loadedDocumentDAO.getHashByFilename(resource.filename());
        return dbHash
                .map(str -> !str.equals(resource.resourceContentHash()))
                .orElse(true);
    }

    private void deleteDocuments(Set<String> filenamesFromDisk){
        List<String> filenamesFromDB = loadedDocumentDAO.getAllFilenames();
        filenamesFromDB.forEach(filenameDB ->{
            if(!filenamesFromDisk.contains(filenameDB)){
                deleteFromVectorStoreByName(filenameDB);
                loadedDocumentDAO.deleteDocumentByName(filenameDB);
                log.info("file: {} removed from db and vector store since it wasn't found on the disk", filenameDB);
            }
        });

    }

    private void deleteFromVectorStoreByName(String filename){
        vectorStore.delete(String.format("source == '%s'", filename));
    }

}
