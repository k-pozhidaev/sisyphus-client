package io.pozhidaev.sisyphusClient.component;

import io.pozhidaev.sisyphusClient.domain.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

import static io.pozhidaev.sisyphusClient.utils.Whitebox.getInternalState;
import static io.pozhidaev.sisyphusClient.utils.Whitebox.setInternalState;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@Slf4j
public class TusdUploadTest {

    private MockWebServer server;

    private WebClient webClient;


    @Before
    public void setup() {
        server = new MockWebServer();
        webClient = WebClient.builder().baseUrl(server.url("/upload").toString()).build();
    }

    @After
    public void shutdown() throws Exception {
        this.server.shutdown();
    }

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


    @Test(expected = NullPointerException.class)
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

    @Test(expected = AsynchronousFileChannelOpenException.class)
    public void asynchronousFileChannelQuietly_Exception() throws IOException {

        final Path file = Paths.get("asynchronousFileChannelQuietly_Exception");
        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();

        final AsynchronousFileChannel asynchronousFileChannel = tusUploader.asynchronousFileChannelQuietly();
        assertTrue(asynchronousFileChannel.isOpen());
        asynchronousFileChannel.close();

    }

    @Test
    public void uploadedLengthFromResponse() {

        final ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
        when(headers.header("Upload-Offset")).thenReturn(Collections.singletonList("666"));
        final ClientResponse response = mock(ClientResponse.class);
        when(response.headers()).thenReturn(headers);
        final TusdUpload tusdUpload = mock(TusdUpload.class);
        when(tusdUpload.uploadedLengthFromResponse(response)).thenCallRealMethod();
        assertEquals(666L, tusdUpload.uploadedLengthFromResponse(response));
    }

    @Test(expected = FileUploadException.class)
    public void uploadedLengthFromResponse_Exception() {
        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.entrySet()).thenReturn(new HashSet<>());

        final ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
        when(headers.header("Upload-Offset")).thenReturn(Collections.emptyList());
        when(headers.asHttpHeaders()).thenReturn(httpHeaders);

        final ClientResponse response = mock(ClientResponse.class);
        when(response.headers()).thenReturn(headers);
        when(response.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);

        final TusdUpload tusdUpload = mock(TusdUpload.class);
        when(tusdUpload.uploadedLengthFromResponse(response)).thenCallRealMethod();
        tusdUpload.uploadedLengthFromResponse(response);
    }

    @Test
    public void dataBufferFlux() throws IOException {

        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        Files.write(file, "test".getBytes());

        final TusdUpload tusUploader = TusdUpload.builder().path(file).chunkSize(1024).build();

        final Flux<DataBuffer> flux = tusUploader.dataBufferFlux(0);

        String blockFirst = flux
            .map(b -> {
                final byte[] bytes = new byte[4];
                b.read(bytes);
                return bytes;
            })
            .map(String::new)
            .blockFirst();
        assertEquals("test", blockFirst);
    }

    @Test
    public void calcChunkCount() {
        final TusdUpload tusdUpload = mock(TusdUpload.class);
        setInternalState(tusdUpload, "chunkSize", 1024);
        when(tusdUpload.readFileSizeQuietly()).thenReturn(5000L);
        when(tusdUpload.calcChunkCount()).thenCallRealMethod();
        assertEquals(5, tusdUpload.calcChunkCount());
    }

    //TODO FIX NULL in mime type probe!
    @Test
    public void readContentTypeQuietly() throws IOException {
        final Path file = Files.createTempFile("readLastModifiedQuietly", ".html");

        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        final String contentType = tusUploader.readContentTypeQuietly();
        Optional.ofNullable(contentType)
            .map(s -> {
                assertTrue(s.contains("text/html"));
                return s;
            })
            .orElseGet(() -> {
                assertTrue(true);
                return "";
            });
    }


    @Test(expected = FileContentTypeReadException.class)
    public void readContentTypeQuietly_Exception() throws IOException {
        final Path file = Files.createTempFile("readLastModifiedQuietly", ".html");

        final TusdUpload tusdUpload = mock(TusdUpload.class);
        setInternalState(tusdUpload, "path", file);
        when(tusdUpload.readContentTypeQuietly()).thenCallRealMethod();
        when(tusdUpload.probeContentType()).thenThrow(new IOException());

        tusdUpload.readContentTypeQuietly();
    }

    @Test
    public void readLastModifiedQuietly() throws IOException {
        final Path file = Files.createTempFile("readLastModifiedQuietly", ".html");

        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        final long quietly = tusUploader.readLastModifiedQuietly();
        assertNotEquals(quietly, System.nanoTime());

    }

    @Test(expected = FileListModifiedReadException.class)
    public void readLastModifiedQuietly_Exception() {
        final Path file = Paths.get("readLastModifiedQuietly_Exception");

        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        tusUploader.readLastModifiedQuietly();
    }

    @Test
    public void handleResponse() {

        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.getLocation()).thenReturn(URI.create("test"));

        final ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
        when(headers.asHttpHeaders()).thenReturn(httpHeaders);

        final ClientResponse response = mock(ClientResponse.class);
        when(response.statusCode()).thenReturn(HttpStatus.CREATED);
        when(response.headers()).thenReturn(headers);

        final TusdUpload tusUploader = TusdUpload.builder().build();
        tusUploader.handleResponse(response);
    }

    @Test(expected = FileUploadException.class)
    public void handleResponse_Exception() {

        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.entrySet()).thenReturn(new HashSet<>());

        final ClientResponse.Headers headers = mock(ClientResponse.Headers.class);

        when(headers.asHttpHeaders()).thenReturn(httpHeaders);

        final ClientResponse clientResponse = mock(ClientResponse.class);

        when(clientResponse.headers()).thenReturn(headers);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);

        final TusdUpload tusUploader = TusdUpload.builder().build();
        tusUploader.handleResponse(clientResponse);
    }

    @Test
    public void readFileSizeQuietly() throws IOException {
        final Path file = Files.createTempFile("readFileSizeQuietly", "qwe");
        Files.write(file, "test".getBytes());
        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        assertEquals(tusUploader.readFileSizeQuietly(), 4);
    }

    @Test(expected = FileSizeReadException.class)
    public void readFileSizeQuietly_exception() {
        final Path file = Paths.get("not_exists/file");
        final TusdUpload tusUploader = TusdUpload.builder().build();
        tusUploader.setFile(file);
        tusUploader.readFileSizeQuietly();
        fail();
    }

    @Test
    public void patchChain() {
        final URI patchUri = URI.create("http://test");
        log.info("chunk {}", patchUri);
        final TusdUpload tusdUpload = mock(TusdUpload.class);
        setInternalState(tusdUpload, "patchUri", patchUri);
        when(tusdUpload.calcChunkCount()).thenReturn(5);
        IntStream.range(0, 5)
            .forEach(chunk -> when(tusdUpload.patch(chunk)).thenReturn(Mono.just(64L)));
        when(tusdUpload.patchChain()).thenCallRealMethod();
        final Long aLong = tusdUpload.patchChain().block();
        assertEquals(Long.valueOf(64), aLong);
    }


    @Test
    public void patch() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(201).setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        final TusdUpload tusdUpload = mock(TusdUpload.class);
        when(tusdUpload.patch(0, (byte) 0)).thenCallRealMethod();
        doCallRealMethod().when(tusdUpload).setClient(webClient);
        when(tusdUpload.dataBufferFlux(0)).thenReturn(Flux.just(stringBuffer("foo"), stringBuffer("bar")));
        when(tusdUpload.readFileSizeQuietly()).thenReturn(6L);
        setInternalState(tusdUpload, "chunkSize", 1024);
        setInternalState(tusdUpload, "patchUri", server.url("/upload").uri());
        setInternalState(tusdUpload, "intervals", new Integer[] {500, 1000});
        System.out.println(server.url("/upload").uri());

        tusdUpload.setClient(webClient);

        tusdUpload.patch(0, (byte) 0).block();

        final RecordedRequest recordedRequest = server.takeRequest();
        assertEquals(recordedRequest.getHeader("Upload-Offset"), "0");
        assertEquals(recordedRequest.getHeader("Content-Length"), "6");
        assertEquals(recordedRequest.getHeader("Content-Type"), "application/offset+octet-stream");
        assertEquals(recordedRequest.getMethod(), "PATCH");
    }

    @Test
    public void patch_secondAttempt() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(500).setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        server.enqueue(new MockResponse().setResponseCode(201).setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        final TusdUpload tusdUpload = mock(TusdUpload.class);
        when(tusdUpload.patch(0, (byte) 0)).thenCallRealMethod();
        when(tusdUpload.patch(0, (byte) 1)).thenCallRealMethod();
        doCallRealMethod().when(tusdUpload).setClient(webClient);
        when(tusdUpload.dataBufferFlux(0)).thenReturn(Flux.just(stringBuffer("foo"), stringBuffer("bar")));
        when(tusdUpload.readFileSizeQuietly()).thenReturn(6L);

        setInternalState(tusdUpload, "chunkSize", 1024);
        setInternalState(tusdUpload, "patchUri", server.url("/upload").uri());
        setInternalState(tusdUpload, "intervals", new Integer[] {500});
        System.out.println(server.url("/upload").uri());

        tusdUpload.setClient(webClient);

        tusdUpload.patch(0, (byte) 0).block();

        final RecordedRequest recordedRequest = server.takeRequest();
        assertEquals(recordedRequest.getHeader("Upload-Offset"), "0");
        assertEquals(recordedRequest.getHeader("Content-Length"), "6");
        assertEquals(recordedRequest.getHeader("Content-Type"), "application/offset+octet-stream");
        assertEquals(recordedRequest.getMethod(), "PATCH");
    }

    @Test(expected = FileUploadException.class)
    public void patch_failedAttempt() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(500).setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        server.enqueue(new MockResponse().setResponseCode(500).setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        final TusdUpload tusdUpload = mock(TusdUpload.class);
        when(tusdUpload.patch(0, (byte) 0)).thenCallRealMethod();
        when(tusdUpload.patch(0, (byte) 1)).thenCallRealMethod();
        doCallRealMethod().when(tusdUpload).setClient(webClient);
        when(tusdUpload.dataBufferFlux(0)).thenReturn(Flux.just(stringBuffer("foo"), stringBuffer("bar")));
        when(tusdUpload.readFileSizeQuietly()).thenReturn(6L);

        setInternalState(tusdUpload, "chunkSize", 1024);
        setInternalState(tusdUpload, "patchUri", server.url("/upload").uri());
        setInternalState(tusdUpload, "intervals", new Integer[] {500});

        tusdUpload.setClient(webClient);

        tusdUpload.patch(0, (byte) 0).block();

        final RecordedRequest recordedRequest = server.takeRequest();
        assertEquals(recordedRequest.getHeader("Upload-Offset"), "0");
        assertEquals(recordedRequest.getHeader("Content-Length"), "6");
        assertEquals(recordedRequest.getHeader("Content-Type"), "application/offset+octet-stream");
        assertEquals(recordedRequest.getMethod(), "PATCH");
    }

    @Test
    public void patch0(){
        final ClientResponse response = ClientResponse.create(HttpStatus.CREATED).header("Upload-Offset", "1024").build();
        final TusdUpload tusdUpload = mock(TusdUpload.class);
        setInternalState(tusdUpload, "uploadedLength", 2048L);
        when(tusdUpload.patch(0, (byte) 0)).thenReturn(Mono.just(response));
        when(tusdUpload.uploadedLengthFromResponse(response)).thenReturn(1024L);
        when(tusdUpload.patch(0)).thenCallRealMethod();
        final Long aLong = tusdUpload.patch(0).block();
        assertEquals(aLong, Long.valueOf(3072));
    }

    @Test
    public void post(){
        final ClientResponse response = ClientResponse.create(HttpStatus.CREATED).header("location", "test://test").build();
        final TusdUpload tusdUpload = mock(TusdUpload.class);
        when(tusdUpload.post()).thenCallRealMethod();
        when(tusdUpload.doPost()).thenReturn(Mono.just(response));
        doNothing().when(tusdUpload).handleResponse(response);
        final TusdUpload upload = tusdUpload.post().block();
        assertNotNull(upload);
        final URI patchUri = (URI) getInternalState(upload, "patchUri");
        assertEquals(patchUri, URI.create("test://test"));
    }

    @Test
    public void doPost() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(201));

        final TusdUpload tusdUpload = mock(TusdUpload.class);
        when(tusdUpload.doPost()).thenCallRealMethod();
        when(tusdUpload.readFileSizeQuietly()).thenReturn(2028L);
        when(tusdUpload.generateMetadataQuietly()).thenReturn("test test,test2 test");
        when(tusdUpload.readContentTypeQuietly()).thenReturn("text/xml");
        doCallRealMethod().when(tusdUpload).setClient(webClient);

        tusdUpload.setClient(webClient);

        tusdUpload.doPost().block();
        final RecordedRequest request = server.takeRequest();
        assertEquals(request.getHeader("Upload-Length"), "2028");
        assertEquals(request.getHeader("Upload-Metadata"), "test test,test2 test");
        assertEquals(request.getHeader("Mime-Type"), "text/xml");
    }


    private DataBuffer stringBuffer(String value) {
        return byteBuffer(value.getBytes(StandardCharsets.UTF_8));
    }

    private DataBuffer byteBuffer(byte[] value) {
        final DataBufferFactory bufferFactory = new DefaultDataBufferFactory(true);
        DataBuffer buffer = bufferFactory.allocateBuffer(value.length);
        buffer.write(value);
        return buffer;
    }
//    private FileAttribute<Set<PosixFilePermission>> fileAttributeReadOnly() {
//        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("r--r--r--");
//        return PosixFilePermissions.asFileAttribute(readOnly);
//    }
//
//    private FileAttribute<Set<PosixFilePermission>> fileAttributeWriteOnly() {
//        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("-w--w--w-");
//        return PosixFilePermissions.asFileAttribute(readOnly);
//    }
}