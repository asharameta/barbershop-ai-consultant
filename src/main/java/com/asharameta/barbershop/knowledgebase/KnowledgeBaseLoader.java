package com.asharameta.barbershop.knowledgebase;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.DigestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.asharameta.barbershop.knowledgebase.BarbershopFileParser.parseFileName;


@AllArgsConstructor
public class KnowledgeBaseLoader {
    private final String resourcePattern;
    private final TextSplitter splitter;
    private final ResourcePatternResolver resolver;

    @SneakyThrows
    List<KnowledgeBaseData> getResources(){
        List<Resource> resources = Arrays.stream(resolver.getResources(resourcePattern)).toList();

        if (resources.isEmpty()) {
            throw new IllegalStateException("No documents found in "+resourcePattern+" directory.");
        }

        return resources.stream().map(resource ->{
            String filename = resource.getFilename();
            BarbershopMetadata metadata = parseFileName(filename);
            return new KnowledgeBaseData(resource,calcContentHash(resource),filename,metadata);
        }).toList();
    }

    List<Document> getChunks(KnowledgeBaseData knowledgeBase){
        List<Document> documents = readDocuments(knowledgeBase.resource());
        stamp(documents, knowledgeBase.metadata());
        return splitter.apply(documents);
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

    @SneakyThrows
    private String calcContentHash(Resource resource){
        return DigestUtils.md5DigestAsHex(resource.getInputStream());
    }
}
