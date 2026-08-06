package com.asharameta.barbershop.knowledgebase;

import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.core.io.Resource;

import org.springframework.ai.document.Document;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

import static com.asharameta.barbershop.knowledgebase.BarbershopFileParser.parseFileName;
import com.asharameta.barbershop.knowledgebase.BarbershopFileParser.BarbershopMetadata;
import org.springframework.core.io.support.ResourcePatternResolver;


public class KnowledgeBaseLoader {
    private final String resourcePattern;
    private final TextSplitter splitter;
    private final ResourcePatternResolver resolver;

    private record KnowledgeBaseData(Resource resource, String filename, BarbershopMetadata metadata) {}

    public KnowledgeBaseLoader(String resourcePattern, TextSplitter textSplitter, ResourcePatternResolver resolver){
        this.resourcePattern = resourcePattern;
        this.splitter = textSplitter;
        this.resolver = resolver;
    }

    public List<Document> loadDocuments(){
        try {
            Resource[] resources = resolver.getResources(resourcePattern);

            if (resources.length == 0) {
                throw new IllegalStateException("No documents found in "+resourcePattern+" directory.");
            }

            List<KnowledgeBaseData> classifieds = Arrays.stream(resources).map(resource -> {
                String filename = resource.getFilename();
                BarbershopMetadata metadata = parseFileName(filename);
                return new KnowledgeBaseData(resource,filename,metadata);
            }).toList();

            List<String> brokenFileNames = classifieds.stream()
                    .filter(classified -> classified.metadata() == null)
                    .map(KnowledgeBaseData::filename).toList();

            if(!brokenFileNames.isEmpty()){
                throw new IllegalStateException("There is files that are not matching the pattern: {barbershop}__{city}__{category}. \n: "+brokenFileNames);
            }

            return classifieds.stream().flatMap(classified -> {
                List<Document> documents = readDocuments(classified.resource());
                stamp(documents, classified.metadata());
                return splitter.split(documents).stream();
            }).toList();

        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to resolve knowledge base resources for pattern: " + resourcePattern, e);
        }
    }

    private List<Document> readDocuments(Resource resource) {
        try {
            return new TikaDocumentReader(resource).read();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to read knowledge base document: " + resource.getFilename(), e);
        }
    }

    private void stamp(List<Document> documents, BarbershopMetadata metadata){
        documents.forEach(doc -> {
            doc.getMetadata().put("barbershop_name", metadata.name().toLowerCase(Locale.ROOT));
            doc.getMetadata().put("barbershop_city", metadata.city().toLowerCase(Locale.ROOT));
            doc.getMetadata().put("barbershop_category", metadata.category().toLowerCase(Locale.ROOT));
        });
    }
}
