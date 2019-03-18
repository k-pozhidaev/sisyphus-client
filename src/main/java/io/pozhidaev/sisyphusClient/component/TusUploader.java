package io.pozhidaev.sisyphusClient.component;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;


@Slf4j
@Service
public class TusUploader implements ApplicationRunner {

    private Options options;

    private Flux<Path> createdFileStream;
    private Supplier<WebClient> webClientFactoryMethod;
    private Supplier<Integer> chunkSize;
    private TusdUpload.TusdUploadBuilder tusdUploadBuilder;

    @Autowired
    public void setCreatedFileStream(Flux<Path> createdFileStream) {
        this.createdFileStream = createdFileStream;
    }

    @Autowired
    public void setWebClientFactoryMethod(final Supplier<WebClient> webClientFactoryMethod) {
        this.webClientFactoryMethod = webClientFactoryMethod;
    }

    @Autowired
    public void setChunkSize(final Supplier<Integer> chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Autowired
    public void tusdUploadBuilder(final TusdUpload.TusdUploadBuilder tusdUploadBuilder) {
        this.tusdUploadBuilder = tusdUploadBuilder;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        final Flux<Path> uriFlux = webClientFactoryMethod
            .get()
            .options()
            .exchange()
            .map(ClientResponse::headers)
            .map(this::buildOptions)
            .doOnNext(o -> this.options = o)
            .thenMany(createdFileStream)
            ;

        final Path path = Paths.get("/Users/i337731/.v8flags.5.5.372.42.i337731.json");


        final TusdUpload upload = tusdUploadBuilder
                .path(path)
                .chunkSize(chunkSize.get())
                .build();

        final Mono<URI> startUploadMono = upload.post().map(cr -> cr.headers().asHttpHeaders().getLocation());

//        final Mono<Long> uploadChain = Mono.zip(startUploadMono, Mono.just(path))
//                .map(t -> Tuples.of(t.getT1(), t.getT2(), t.getT2()))
//                .map(t -> t.mapT3(p -> IntStream.range(0, calcChunkCount(p))))
//                .flatMap(t -> t.getT3().mapToObj(o -> uploadChunk(o, t.getT1(), t.getT2())).reduce(Mono::then).orElse(Mono.empty()));
//            .subscribe()


//        uploadChunk(0, URI.create("http://localhost:8080/upload/21"), path).subscribe();
//        dataBufferFlux(0, path).doOnNext(dataBuffer -> log.info("SIZE: {}", dataBuffer.capacity())).subscribe();
//        log.info("test");
    }

    private Options buildOptions(final ClientResponse.Headers headers) {
        final Options options = new Options();

        getZeroElementIfExists(headers.header(Options.HEADER_NAME_RESUMABLE))
            .ifPresent(options::setResumable);
        getZeroElementIfExists(headers.header(Options.HEADER_NAME_VERSION))
            .map(s -> s.split(","))
            .map(Arrays::asList)
            .ifPresent(options::setVersion);
        getZeroElementIfExists(headers.header(Options.HEADER_NAME_EXTENSION))
            .map(s -> s.split(","))
            .map(Arrays::asList)
            .ifPresent(options::setExtension);
        getZeroElementIfExists(headers.header(Options.HEADER_NAME_MAX_SIZE))
            .map(Long::valueOf)
            .ifPresent(options::setMaxSize);
        return options;
    }


    @Setter
    @ToString
    @NoArgsConstructor
    private class Options {
        public final static String HEADER_NAME_RESUMABLE = "Tus-Resumable";
        public final static String HEADER_NAME_VERSION = "Tus-Version";
        public final static String HEADER_NAME_EXTENSION = "Tus-Extension";
        public final static String HEADER_NAME_MAX_SIZE = "Tus-Max-Size";

        private String resumable;
        private List<String> version;
        private List<String> extension;
        private Long maxSize;

    }

    private static <T> Optional<T> getZeroElementIfExists(final List<T> list) {
        if (list.size() > 0) {
            return Optional.of(list.get(0));
        }
        return Optional.empty();
    }


    static class FileSizeReadException extends RuntimeException {

        FileSizeReadException(Path path, Throwable cause) {
            super(String.format("File size read exception: %s", path.toAbsolutePath()), cause);
        }
    }

    static class FileContentTypeReadException extends RuntimeException {

        FileContentTypeReadException(Path path, Throwable cause) {
            super(String.format("File content type read exception: %s", path.toAbsolutePath()), cause);
        }
    }

    static class AsynchronousFileChannelOpenException extends RuntimeException {
        AsynchronousFileChannelOpenException(Path path, Throwable cause) {
            super(String.format("File content read exception: %s", path.toAbsolutePath()), cause);
        }
    }

    static class TusNotComplientExeption extends RuntimeException {
        TusNotComplientExeption() {
            super("Upload offset header did not set up.");
        }
    }

}
