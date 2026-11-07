package com.asharameta.barbershop.knowledgebase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseIngestorTest {
    @Mock
    private VectorStore vectorStore;
    @Mock
    private LoadedDocumentDAO loadedDocumentDAO;
    @Mock
    private KnowledgeBaseLoader loader;
    @InjectMocks
    private KnowledgeBaseIngestor ingestor;

    private static KnowledgeBaseData data(String filename) {
        return new KnowledgeBaseData(null, "hash", filename, BarbershopFileParser.parseFileName(filename));
    }

    @DisplayName("Already loaded file is skipped")
    @Test
    void skipsAlreadyLoadedFile() {
        var file = data("name__city__about.txt");
        when(loader.getResources()).thenReturn(List.of(file));
        when(loadedDocumentDAO.existByFilenameAndContentHash("name__city__about.txt", "hash")).thenReturn(true);

        ingestor.ingest();

        verify(vectorStore, never()).accept(any());
        verify(loadedDocumentDAO, never()).loadDocument(any());
    }

    @DisplayName("New file goes to vector store, then to the registry")
    @Test
    void loadsNewFile() {
        var file = data("name__city__about.txt");
        List<Document> chunks = List.of(new Document("a"), new Document("b"));
        when(loader.getResources()).thenReturn(List.of(file));
        when(loadedDocumentDAO.existByFilenameAndContentHash("name__city__about.txt", "hash")).thenReturn(false);
        when(loader.getChunks(file)).thenReturn(chunks);

        ingestor.ingest();

        var order = inOrder(vectorStore, loadedDocumentDAO);
        order.verify(vectorStore).accept(chunks);
        var captor = ArgumentCaptor.forClass(LoadedDocument.class);
        order.verify(loadedDocumentDAO).loadDocument(captor.capture());

        LoadedDocument saved = captor.getValue();
        assertEquals("name__city__about.txt", saved.getFilename());
        assertEquals("hash", saved.getContentHash());
        assertEquals(".txt", saved.getDocumentType());
        assertEquals(2, saved.getChunkCount());
    }

    @DisplayName("File with a wrong name is skipped")
    @Test
    void skipsBrokenFilename() {
        when(loader.getResources()).thenReturn(List.of(data("broken_name.txt")));

        ingestor.ingest();

        verify(loader, never()).getChunks(any());
        verify(vectorStore, never()).accept(any());
        verify(loadedDocumentDAO, never()).loadDocument(any());
    }
}