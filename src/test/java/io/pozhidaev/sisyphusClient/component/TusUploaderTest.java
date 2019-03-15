package io.pozhidaev.sisyphusClient.component;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.nio.file.StandardOpenOption.*;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class TusUploaderTest {

    @MockBean
    Supplier<WebClient> webClientFactoryMethod;
    WebClient client;

    @MockBean
    Flux<Path> createdFileStream;

    @MockBean
    Supplier<Integer> chunkSize;

    @Before
    public void onInit(){
        client = Mockito.mock(WebClient.class);
        Mockito.when(webClientFactoryMethod.get()).thenReturn(client);
    }


    @Test
    public void generateMetadataQuietly() throws IOException {
        final Path file = Files.createTempFile("generateMetadataQuietly", " 1");

        final TusUploader tusUploader = new TusUploader();
        final String metadata = tusUploader.generateMetadataQuietly(file);
        final String encodeToString = Base64.getEncoder().encodeToString(file.getFileName().toString().getBytes());
        assertTrue(metadata.contains(encodeToString));
        assertTrue(metadata.contains("filename"));
    }

    @Test
    public void asynchronousFileChannelQuietly() throws IOException {

        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");

        final TusUploader tusUploader = new TusUploader();

        final AsynchronousFileChannel asynchronousFileChannel = tusUploader.asynchronousFileChannelQuietly(file);
        assertTrue(asynchronousFileChannel.isOpen());
        asynchronousFileChannel.close();

    }

    @Test
    public void calcFingerprint() throws IOException {
        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        Files.write(file, "Test,test,test".getBytes());
        final TusUploader tusUploader = new TusUploader();

        final String fingerprint = tusUploader.calcFingerprint(file);
        assertTrue(fingerprint.contains("14"));
        System.out.println(fingerprint);
    }

    @Test
    @Ignore
    public void makeTestFile() throws IOException {
        final StringBuffer buffer = new StringBuffer();
        final Path path = Paths.get("./20MB.txt");
        Files.deleteIfExists(path);
        final int sum = IntStream.rangeClosed(97, 122)
                .flatMap(v -> IntStream.rangeClosed(1, 1024)
                        .map(i -> i % 128 == 0 ? 10 : v)
                        .peek(i -> buffer.append((char) i))
                )
                .map(operand -> 1)
                .sum();
        System.out.println(buffer.length());
        buffer.deleteCharAt(buffer.length()-1);
        System.out.println(buffer);
        Files.write(path, Collections.singleton(buffer), CREATE);
        System.out.println(sum);
        Assert.assertEquals( 1024 * 26, Files.size(path));
    }
}