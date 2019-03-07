package io.pozhidaev.sisyphusClient.component;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class TusUploaderTest {

    @MockBean
    WebClient webClient;
    @MockBean
    Flux<Path> createdFileStream;


    @Test
    public void generateMetadataSilencely() throws IOException {
        final Path file = Files.createTempFile("generateMetadataSilencely", " 1");

        final TusUploader tusUploader = new TusUploader(webClient, createdFileStream);
        final String metadata = tusUploader.generateMetadataSilencely(file);
        final String encodeToString = Base64.getEncoder().encodeToString(file.getFileName().toString().getBytes());
        assertTrue(metadata.contains(encodeToString));
        assertTrue(metadata.contains("filename"));
    }
}