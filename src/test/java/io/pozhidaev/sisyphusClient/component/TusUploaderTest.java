package io.pozhidaev.sisyphusClient.component;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.function.Supplier;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class TusUploaderTest {

    @MockBean
    Supplier<WebClient> webClientFactoryMethod;
    WebClient client;

    @MockBean
    Flux<Path> createdFileStream;

    @Before
    public void onInit(){
        client = Mockito.mock(WebClient.class);
        Mockito.when(webClientFactoryMethod.get()).thenReturn(client);
    }


    @Test
    public void generateMetadataQuietly() throws IOException {
        final Path file = Files.createTempFile("generateMetadataQuietly", " 1");

        final TusUploader tusUploader = new TusUploader(webClientFactoryMethod, createdFileStream) ;
        final String metadata = tusUploader.generateMetadataQuietly(file);
        final String encodeToString = Base64.getEncoder().encodeToString(file.getFileName().toString().getBytes());
        assertTrue(metadata.contains(encodeToString));
        assertTrue(metadata.contains("filename"));
    }
}