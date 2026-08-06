package com.asharameta.barbershop.knowledgebase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseLoaderTest {
    private final String resourcePattern = "classpath:/docs/**";
    @Mock
    private TextSplitter splitter;
    @Mock
    private ResourcePatternResolver resolver;
    KnowledgeBaseLoader knowledgeBaseLoader;

    @BeforeEach
    void init() {
        knowledgeBaseLoader = new KnowledgeBaseLoader(resourcePattern, splitter, resolver);
    }


    private static class FakeResource extends ByteArrayResource {
        private final String filename;

        public FakeResource(String filename){
            super("FAKE BODY".getBytes(StandardCharsets.UTF_8));
            this.filename = filename;
        }

        @Override
        public String getFilename(){
            return filename;
        }
    }


    @DisplayName("Test for documents list is not empty")
    @Test
    void testDocumentsIsNotEmpty() throws IOException {
        String filename = "name__city__category.txt";
        when(resolver.getResources(resourcePattern)).thenReturn(new Resource[]{new FakeResource(filename)});
        when(splitter.split(anyList())).thenAnswer(inv -> inv.getArgument(0));
        var documents = knowledgeBaseLoader.loadDocuments();

        assertNotEquals(0, documents.size());
    }

    @DisplayName("Test to check all metadata parsed correctly")
    @Test
    void testDocumentsHasCorrectData() throws IOException {
        String filename = "name__city__category.txt";
        when(resolver.getResources(resourcePattern)).thenReturn(new Resource[]{new FakeResource(filename)});
        when(splitter.split(anyList())).thenAnswer(inv -> inv.getArgument(0));
        var documents = knowledgeBaseLoader.loadDocuments();

        Map<String, Object> metadata = documents.getFirst().getMetadata();
        assertEquals("name", metadata.get("barbershop_name"));
        assertEquals("city", metadata.get("barbershop_city"));
        assertEquals("category", metadata.get("barbershop_category"));
    }

    @DisplayName("Test to check wrong filename")
    @Test
    void TryToParseWrongFilenameTest() throws IOException {
        String wrongFilename1 = "name_city___category.txt";
        String wrongFilename2 = "double_name__city__category.txt";
        when(resolver.getResources(resourcePattern)).thenReturn(new Resource[]{new FakeResource(wrongFilename1),new FakeResource(wrongFilename2), new FakeResource(null)});

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> knowledgeBaseLoader.loadDocuments());

        assertTrue(ex.getMessage().contains("There is files that are not matching the pattern: {barbershop}__{city}__{category}."));
    }

    @DisplayName("Test to check wrong resourcePattern")
    @Test
    void testResolverIOExceptionIsWrappedAsUncheckedIOException() throws IOException {
        var ioException = new IOException("Failed to resolve knowledge base resources for pattern: " + resourcePattern);
        when(resolver.getResources(resourcePattern)).thenThrow(ioException);

        UncheckedIOException exception = assertThrows(UncheckedIOException.class,
                () -> knowledgeBaseLoader.loadDocuments());

        assertEquals(ioException, exception.getCause(), "prove that exception we get is the one from thenThrow");
        assertEquals(ioException.getMessage(), exception.getMessage());
    }

    @DisplayName("Test no resources found throws IllegalStateException")
    @Test
    void testNoResourceFoundException() throws IOException {
        when(resolver.getResources(resourcePattern)).thenReturn(new Resource[0]);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> knowledgeBaseLoader.loadDocuments());
        assertEquals("No documents found in "+resourcePattern+" directory.", ex.getMessage());
    }


    private static class BrokenResource extends ByteArrayResource {
        private final String filename;

        public BrokenResource(String filename){
            super(new byte[0]);
            this.filename = filename;
        }

        @Override
        public InputStream getInputStream() throws IOException { throw new IOException("Failed to read knowledge base document: " + filename); }

        @Override
        public String getFilename(){
            return filename;
        }
    }

    @DisplayName("Test to check Tika read failure is wrapped as IllegalStateException")
    @Test
    void testTikaReadFailureIsWrappedAsIllegalStateException() throws IOException {
        String filename = "name__city__category.txt";
        when(resolver.getResources(resourcePattern)).thenReturn(new Resource[]{new BrokenResource(filename)});

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> knowledgeBaseLoader.loadDocuments());
        assertEquals("Failed to read knowledge base document: " + filename, ex.getMessage());
    }

}