package io.pozhidaev.sisyphusClient.component;

import io.pozhidaev.sisyphusClient.utils.Whitebox;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static io.pozhidaev.sisyphusClient.utils.Whitebox.setInternalState;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TusdUploadTest {

    @Test
    public void calcFingerprint() throws IOException {
        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        Files.write(file, "Test,test,test".getBytes());
        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();

        final String fingerprint = tusUploader.calcFingerprint();
        assertTrue(fingerprint.contains("14"));
        System.out.println(fingerprint);
    }

    @Test(expected = NullPointerException.class)
    public void calcFingerprint_nullPointerException() throws IOException {
        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        Files.write(file, "Test,test,test".getBytes());
        final TusdUpload tusUploader = TusdUpload.builder().build();

        final String fingerprint = tusUploader.calcFingerprint();
        assertTrue(fingerprint.contains("14"));
        System.out.println(fingerprint);
    }


    @Test
    public void generateMetadataQuietly() throws IOException {
        final Path file = Files.createTempFile("generateMetadataQuietly", " 1");

        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        final String metadata = tusUploader.generateMetadataQuietly();
        final String encodeToString = Base64.getEncoder().encodeToString(file.getFileName().toString().getBytes());
        assertTrue(metadata.contains(encodeToString));
        assertTrue(metadata.contains("filename"));
    }


    @Test(expected =  NullPointerException.class)
    public void generateMetadataQuietly_nullPointerException() throws IOException {
        final Path file = Files.createTempFile("generateMetadataQuietly", " 1");

        final TusdUpload tusUploader = TusdUpload.builder().build();
        final String metadata = tusUploader.generateMetadataQuietly();
        final String encodeToString = Base64.getEncoder().encodeToString(file.getFileName().toString().getBytes());
        assertTrue(metadata.contains(encodeToString));
        assertTrue(metadata.contains("filename"));
    }

    @Test
    public void asynchronousFileChannelQuietly() throws IOException {

        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();

        final AsynchronousFileChannel asynchronousFileChannel = tusUploader.asynchronousFileChannelQuietly();
        assertTrue(asynchronousFileChannel.isOpen());
        asynchronousFileChannel.close();

    }

    @Test(expected = RuntimeException.class)
    public void tryItOut(){
        Assert.assertEquals(Mono.just(1).block(), Integer.valueOf(1));
        Mono.error(new RuntimeException("nice try")).block();
    }

    @Test
    public void retryablePatch() {
        final Integer[] intervals = {500, 1000};
        final long res =  15;
        final ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode())
                .thenReturn(HttpStatus.BAD_REQUEST)
                .thenReturn(HttpStatus.CREATED);

        final TusdUpload tusdUpload = mock(TusdUpload.class);
        URI uri = URI.create("http://localhost:1111/u/1");
        setInternalState(tusdUpload, "intervals", intervals);

        when(tusdUpload.patchErrorHandling(clientResponse)).thenReturn(Long.MIN_VALUE);
        when(tusdUpload.uploadedLengthFromResponse(clientResponse)).thenReturn(res);
        when(tusdUpload.patch(0, uri)).thenReturn(Mono.just(clientResponse));
        when(tusdUpload.retryablePatch(0, uri)).thenCallRealMethod();

        final long result = tusdUpload.retryablePatch(0, uri);
        assertEquals(result, res);

    }

    @Test
    public void retryablePatch_didNotPassed() {
        final Integer[] intervals = {500, 1000};
        final ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode())
                .thenReturn(HttpStatus.BAD_REQUEST)
                .thenReturn(HttpStatus.BAD_REQUEST)
                .thenReturn(HttpStatus.BAD_REQUEST)
        ;

        final TusdUpload tusdUpload = mock(TusdUpload.class);
        URI uri = URI.create("http://localhost:1111/u/1");
        setInternalState(tusdUpload, "intervals", intervals);

        when(tusdUpload.patchErrorHandling(clientResponse))
                .thenReturn(Long.MIN_VALUE)
                .thenReturn(Long.MIN_VALUE)
                .thenReturn(Long.MIN_VALUE)
        ;

        when(tusdUpload.patch(0, uri)).thenReturn(Mono.just(clientResponse));
        when(tusdUpload.retryablePatch(0, uri)).thenCallRealMethod();

        final long result = tusdUpload.retryablePatch(0, uri);
        assertEquals(result, Long.MIN_VALUE);

    }
}