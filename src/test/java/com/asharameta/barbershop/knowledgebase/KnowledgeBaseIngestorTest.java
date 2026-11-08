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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseIngestorTest {
    private static final String FILE = "name__city__about.txt";

    @Mock
    private VectorStore vectorStore;
    @Mock
    private LoadedDocumentDAO loadedDocumentDAO;
    @Mock
    private KnowledgeBaseLoader loader;
    @InjectMocks
    private KnowledgeBaseIngestor ingestor;

    private static KnowledgeBaseData data(String filename, String hash) {
        return new KnowledgeBaseData(null, hash, filename, BarbershopFileParser.parseFileName(filename));
    }

    private static String sourceFilter(String filename) {
        return "source == '" + filename + "'";
    }

    private void verifyReloaded(KnowledgeBaseData file, List<Document> chunks, String expectedHash) {
        var order = inOrder(vectorStore, loadedDocumentDAO);
        order.verify(vectorStore).delete(sourceFilter(file.filename()));
        order.verify(vectorStore).accept(chunks);
        var captor = ArgumentCaptor.forClass(LoadedDocument.class);
        order.verify(loadedDocumentDAO).loadDocument(captor.capture());

        LoadedDocument saved = captor.getValue();
        assertEquals(file.filename(), saved.getFilename());
        assertEquals(expectedHash, saved.getContentHash());
        assertEquals(".txt", saved.getDocumentType());
        assertEquals(chunks.size(), saved.getChunkCount());
    }

    @DisplayName("New file: delete old chunks -> accept -> upsert")
    @Test
    void loadsNewFile() {
        var file = data(FILE, "hash");
        List<Document> chunks = List.of(new Document("a"), new Document("b"));
        when(loader.getResources()).thenReturn(List.of(file));
        when(loadedDocumentDAO.getHashByFilename(FILE)).thenReturn(Optional.empty());
        when(loader.getChunks(file)).thenReturn(chunks);

        ingestor.ingest();

        verifyReloaded(file, chunks, "hash");
    }

    @DisplayName("Unchanged file (same hash) is skipped")
    @Test
    void skipsUnchangedFile() {
        when(loader.getResources()).thenReturn(List.of(data(FILE, "hash")));
        when(loadedDocumentDAO.getHashByFilename(FILE)).thenReturn(Optional.of("hash"));

        ingestor.ingest();

        verify(vectorStore, never()).delete(anyString());
        verify(vectorStore, never()).accept(anyList());
        verify(loadedDocumentDAO, never()).loadDocument(any());
    }

    @DisplayName("Changed file (different hash) is reloaded")
    @Test
    void reloadsChangedFile() {
        var file = data(FILE, "new-hash");
        List<Document> chunks = List.of(new Document("a"));
        when(loader.getResources()).thenReturn(List.of(file));
        when(loadedDocumentDAO.getHashByFilename(FILE)).thenReturn(Optional.of("old-hash"));
        when(loader.getChunks(file)).thenReturn(chunks);

        ingestor.ingest();

        verifyReloaded(file, chunks, "new-hash");
    }

    @DisplayName("File with a wrong name is skipped")
    @Test
    void skipsBrokenFilename() {
        when(loader.getResources()).thenReturn(List.of(data("broken_name.txt", "hash")));

        ingestor.ingest();

        verify(loader, never()).getChunks(any());
        verify(vectorStore, never()).delete(anyString());
        verify(vectorStore, never()).accept(anyList());
        verify(loadedDocumentDAO, never()).loadDocument(any());
    }

    @DisplayName("Restart without changes deletes nothing (regression: everything was wiped)")
    @Test
    void restartWithoutChangesDeletesNothing() {
        when(loader.getResources()).thenReturn(List.of(data(FILE, "hash")));
        when(loadedDocumentDAO.getHashByFilename(FILE)).thenReturn(Optional.of("hash"));
        when(loadedDocumentDAO.getAllFilenames()).thenReturn(List.of(FILE));

        ingestor.ingest();

        verify(loadedDocumentDAO, never()).deleteDocumentByName(anyString());
        verify(vectorStore, never()).delete(anyString());
    }

    @DisplayName("File removed from disk: chunks deleted first, then registry row")
    @Test
    void removesFileMissingOnDisk() {
        String removed = "gone__city__about.txt";
        when(loader.getResources()).thenReturn(List.of(data(FILE, "hash")));
        when(loadedDocumentDAO.getHashByFilename(FILE)).thenReturn(Optional.of("hash"));
        when(loadedDocumentDAO.getAllFilenames()).thenReturn(List.of(FILE, removed));

        ingestor.ingest();

        var order = inOrder(vectorStore, loadedDocumentDAO);
        order.verify(vectorStore).delete(sourceFilter(removed));
        order.verify(loadedDocumentDAO).deleteDocumentByName(removed);
        verify(loadedDocumentDAO, never()).deleteDocumentByName(FILE);
    }
}