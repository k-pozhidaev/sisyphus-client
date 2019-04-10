package io.pozhidaev.sisyphusClient.component;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.boot.ApplicationArguments;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static io.pozhidaev.sisyphusClient.domain.Options.*;
import static io.pozhidaev.sisyphusClient.utils.Whitebox.getInternalState;
import static java.nio.file.StandardOpenOption.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class TusUploaderTest {

    private Supplier<WebClient> webClientFactoryMethod;

    private Flux<Path> createdFileStream;

    private Supplier<Integer> chunkSize;

    private MockWebServer server;

    private WebClient webClient;

    @After
    public void shutdown() throws Exception {
        this.server.shutdown();
    }
    @Before
    public void onInit(){
        server = new MockWebServer();
        webClient = WebClient.builder().baseUrl(server.url("/upload").toString()).build();
        webClientFactoryMethod = () -> webClient;
        createdFileStream = Flux.just(Paths.get("test1"), Paths.get("test2"), Paths.get("test3"));
        chunkSize = () -> 6000;
    }

    @Test
    public void setCreatedFileStream(){
        final Flux<Path> flux = Flux.just(Paths.get("test"));
        final TusUploader tusUploader = new TusUploader();
        tusUploader.setCreatedFileStream(flux);
        assertEquals(getInternalState(tusUploader,"createdFileStream"), flux);
    }

    @Test
    public void setWebClientFactoryMethod(){
        final Supplier<WebClient> webClientFactoryMethod = WebClient::create;
        final TusUploader tusUploader = new TusUploader();
        tusUploader.setWebClientFactoryMethod(webClientFactoryMethod);
        assertEquals(webClientFactoryMethod, getInternalState(tusUploader, "webClientFactoryMethod"));
    }

    @Test
    public void setChunkSize(){
        final Supplier<Integer> chunkSize = () -> Integer.MAX_VALUE;
        final TusUploader tusUploader = new TusUploader();
        tusUploader.setChunkSize(chunkSize);
        assertEquals(chunkSize, getInternalState(tusUploader, "chunkSize"));
    }

    @Test
    public void tusdUploadBuilder(){
        final TusdUpload.Builder tusdUploadBuilder = TusdUpload.builder();
        final TusUploader tusUploader = new TusUploader();
        tusUploader.tusdUploadBuilder(tusdUploadBuilder);
        assertEquals(tusdUploadBuilder, getInternalState(tusUploader, "tusdUploadBuilder"));
    }

    @Test
    public void run(){
        server.enqueue(new MockResponse().setResponseCode(201));
        final TusdUpload upload = mock(TusdUpload.class);
        when(upload.post()).thenReturn(Mono.just(upload));
        when(upload.patchChain()).thenReturn(Mono.just(1000L));
        final ApplicationArguments applicationArguments = mock(ApplicationArguments.class);
        final TusUploader tusUploader = mock(TusUploader.class);
        when(tusUploader.tusdUploadBuild(Paths.get("test1"))).thenReturn(upload);
        when(tusUploader.tusdUploadBuild(Paths.get("test2"))).thenReturn(upload);
        when(tusUploader.tusdUploadBuild(Paths.get("test3"))).thenReturn(upload);
        when(tusUploader.options()).thenReturn(Mono.just(ClientResponse
            .create(HttpStatus.NO_CONTENT)
            .header(HEADER_NAME_RESUMABLE, "test")
            .header(HEADER_NAME_VERSION, "1,2,3")
            .header(HEADER_NAME_EXTENSION, "test1,test2")
            .build()));
        doCallRealMethod().when(tusUploader).setCreatedFileStream(createdFileStream);
        when(tusUploader.tusdUploadBuild(Paths.get("test1"))).thenReturn(upload);
        when(tusUploader.tusdUploadBuild(Paths.get("test2"))).thenReturn(upload);
        when(tusUploader.tusdUploadBuild(Paths.get("test3"))).thenReturn(upload);
        doCallRealMethod().when(tusUploader).run(applicationArguments);

        tusUploader.setCreatedFileStream(createdFileStream);
        tusUploader.run(applicationArguments);
    }

    @Test
    public void options() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(201));
        final TusUploader tusUploader = new TusUploader();
        tusUploader.setWebClientFactoryMethod(webClientFactoryMethod);
        tusUploader.options().block();
        server.takeRequest();
    }

    @Test
    public void tusdUploadBuild(){
        final TusUploader tusUploader = new TusUploader();
        tusUploader.tusdUploadBuilder(TusdUpload.builder());
        tusUploader.setWebClientFactoryMethod(webClientFactoryMethod);
        tusUploader.setChunkSize(chunkSize);
        final TusdUpload tusdUpload = tusUploader.tusdUploadBuild(Paths.get("test"));
        assertEquals(getInternalState(tusdUpload, "path"), Paths.get("test"));
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