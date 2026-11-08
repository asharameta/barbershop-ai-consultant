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
    private KnowledgeBaseLoader knowledgeBaseLoader;

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
        when(splitter.apply(anyList())).thenAnswer(inv -> inv.getArgument(0));
        var resources = knowledgeBaseLoader.getResources();
        var documents = knowledgeBaseLoader.getChunks(resources.getFirst());

        assertNotEquals(0, documents.size());
    }

    @DisplayName("Test to check all metadata parsed correctly")
    @Test
    void testDocumentsHasCorrectData() throws IOException {
        String filename = "name__city__category.txt";
        when(resolver.getResources(resourcePattern)).thenReturn(new Resource[]{new FakeResource(filename)});
        when(splitter.apply(anyList())).thenAnswer(inv -> inv.getArgument(0));
        var resources = knowledgeBaseLoader.getResources();
        var documents = knowledgeBaseLoader.getChunks(resources.getFirst());

        Map<String, Object> metadata = documents.getFirst().getMetadata();
        assertEquals("name", metadata.get("barbershop_name"));
        assertEquals("city", metadata.get("barbershop_city"));
        assertEquals("category", metadata.get("barbershop_category"));
    }

    @DisplayName("Wrong filenames are returned with null metadata")
    @Test
    void tryToParseWrongFilenameTest() throws IOException {
        String wrongFilename1 = "name_city___category.txt";
        String wrongFilename2 = "double_name__city__category.txt";
        when(resolver.getResources(resourcePattern)).thenReturn(new Resource[]{new FakeResource(wrongFilename1),new FakeResource(wrongFilename2), new FakeResource(null)});

        var resources = knowledgeBaseLoader.getResources();

        assertEquals(3, resources.size());
        assertTrue(resources.stream().allMatch(r -> r.metadata() == null));
    }

    @DisplayName("Test to check wrong resourcePattern")
    @Test
    void testResolverIOExceptionIsPropagated() throws IOException {
        var ioException = new IOException("Failed to resolve knowledge base resources for pattern: " + resourcePattern);
        when(resolver.getResources(resourcePattern)).thenThrow(ioException);

        IOException exception = assertThrows(IOException.class,
                () -> knowledgeBaseLoader.getResources());

        assertSame(ioException, exception, "prove that the exception we get is the one from thenThrow");
    }

    @DisplayName("Test that no resources found throws an IllegalStateException")
    @Test
    void testNoResourceFoundException() throws IOException {
        when(resolver.getResources(resourcePattern)).thenReturn(new Resource[0]);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> knowledgeBaseLoader.getResources());
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
        // built by hand: getResources() would fail earlier, in calcContentHash
        var data = new KnowledgeBaseData(new BrokenResource(filename), "hash", filename,
                BarbershopFileParser.parseFileName(filename));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> knowledgeBaseLoader.getChunks(data));
        assertEquals("Failed to read knowledge base document: " + filename, ex.getMessage());
    }

    @DisplayName("Unreadable file fails while hashing in getResources")
    @Test
    void testHashReadFailureIsPropagated() throws IOException {
        String filename = "name__city__category.txt";
        when(resolver.getResources(resourcePattern)).thenReturn(new Resource[]{new BrokenResource(filename)});

        assertThrows(IOException.class, () -> knowledgeBaseLoader.getResources());
    }

}