package io.pozhidaev.sisyphusClient.component;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.READ;


@Slf4j
@Service
public class TusUploader {

    private Options options;

    private Flux<Path> createdFileStream;
    private Supplier<WebClient> webClientFactoryMethod;
    private Supplier<Integer> chunkSize;

    @Autowired
    public void setCreatedFileStream(Flux<Path> createdFileStream) {
        this.createdFileStream = createdFileStream;
    }

    @Autowired
    public void setWebClientFactoryMethod(Supplier<WebClient> webClientFactoryMethod) {
        this.webClientFactoryMethod = webClientFactoryMethod;
    }

    @Autowired
    public void setChunkSize(Supplier<Integer> chunkSize) {
        this.chunkSize = chunkSize;
    }

    @PostConstruct
    public void onInit() {
        //patch:
        //Content-Length
        //Upload-Offset

//        webClientFactoryMethod
//            .get()
//            .options()
//            .startUploadMono()
//            .map(ClientResponse::headers)
//            .map(this::buildOptions)
//            .doOnNext(o -> this.options = o)
//            .thenMany(createdFileStream)
//            .flatMap(path -> webClientFactoryMethod.get().post()
//                    .headers(httpHeaders -> {
//                        httpHeaders.set("Upload-Length", readFileSizeQuietly(path));
//                        httpHeaders.set("Upload-Metadata", generateMetadataQuietly(path));
//                        httpHeaders.set("Mime-Type", readContentTypeQuietly(path));
//                    })
//                    .startUploadMono()
//            )
//            .map(clientResponse -> Objects.requireNonNull(clientResponse.headers().asHttpHeaders().getLocation()))
//            .flatMap(s -> webClientFactoryMethod.get()
//                .patch()
//                .uri(u -> u.path(s.getPath()).build())
//                .body(pathToBuffer(), DataBuffer.class)
//                .header("Upload-Offset", "0")
//                .header("Content-Length", "1024")
//                .startUploadMono()
//            )
//            .subscribe()
//        ;


//        t.getT1().
//                .map(cr -> Objects.requireNonNull(cr.headers().asHttpHeaders().getLocation()))
//            .flatMap(uri -> uploadChunk(uri)
//                .doOnNext(TusUploader::accept)
//            )
        final Path path = Paths.get("/Users/i337731/.v8flags.5.5.372.42.i337731.json");


        final Mono<ClientResponse> startUploadMono = webClientFactoryMethod.get().post()
            .headers(httpHeaders -> {
                httpHeaders.set("Upload-Length", readFileSizeQuietly(path).toString());
                httpHeaders.set("Upload-Metadata", generateMetadataQuietly(path));
                httpHeaders.set("Mime-Type", readContentTypeQuietly(path));
            })
            .exchange();

        Mono.zip(startUploadMono, Mono.just(path))
            .doOnNext(t -> TusUploader.accept(t.getT1()))
            .map(t -> Tuples.of(
                Objects.requireNonNull(t.getT1().headers().asHttpHeaders().getLocation()),
                t.getT2())
            )
            .flatMapMany(t -> Flux.range(0, getChunkCount(t.getT2()))
                .doOnNext(i -> log.info("chunk: {}, url: {}, path: {}.", i, t.getT1(), t.getT2()))
                .flatMap(i -> uploadChunk(i, t.getT1(), t.getT2()))
            )
            .subscribe()
            ;
//        postMono.subscribe();

    }

    int getChunkCount (final Path path) {
        return Long.valueOf(readFileSizeQuietly(path) / chunkSize.get()).intValue() + 1;
    }

    Mono<ClientResponse> uploadChunk(final long chunk, final URI uri, final Path path) {
        return pathToBuffer(chunk, path).flatMap(t -> webClientFactoryMethod.get()
            .patch()
            .uri(uri)
            .body(t.getT1(), DataBuffer.class)
            .header("Upload-Offset", t.getT3().toString())
            .header("Content-Length", t.getT2().toString())
            .header("Content-Type", "application/offset+octet-stream")
            .exchange());
    }

    Mono<Tuple3<Flux<DataBuffer>, Integer, Long>> pathToBuffer(final long chunk, final Path path) {
        final int integer = chunkSize.get();
        final AsynchronousFileChannel channel = asynchronousFileChannelQuietly(path);
        final Flux<DataBuffer> bufferFlux = DataBufferUtils.readAsynchronousFileChannel(() -> channel, chunk * integer, new DefaultDataBufferFactory(), integer);
        return Mono.zip(
            Mono.just(bufferFlux),
            bufferFlux.map(DataBuffer::capacity).reduce(Integer::sum),
            Mono.just(chunk)
        );
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

    private static Long readFileSizeQuietly(final Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            final FileSizeReadException exception = new FileSizeReadException(path, e);
            log.error("Reading file size error.", exception);
            throw exception;
        }
    }

    private static String readContentTypeQuietly(final Path path) {
        try {
            return Files.probeContentType(path);
        } catch (IOException e) {
            final FileContentTypeReadException exception = new FileContentTypeReadException(path, e);
            log.error("Reading content type error.", exception);
            throw exception;

        }
    }

    String generateMetadataQuietly(final Path path) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("filename", path.getFileName().toString());
        metadata.put("fingerprint", calcFingerprint(path));

        return metadata
            .entrySet()
            .stream()
            .map(e -> String.format("%s %s", e.getKey(), Base64.getEncoder().encodeToString(e.getValue().getBytes())))
            .collect(Collectors.joining(","))
            ;
    }

    AsynchronousFileChannel asynchronousFileChannelQuietly(final Path path) {
        try {
            return AsynchronousFileChannel.open(path, READ);
        } catch (IOException e) {
            final AsynchronousFileChannelOpenException exception = new AsynchronousFileChannelOpenException(path, e);
            log.error("File read error.", exception);
            throw exception;
        }
    }

    String calcFingerprint(final Path path) {
        return String.format("%s-%s", path.toAbsolutePath(), readFileSizeQuietly(path));
    }

    private static void accept(ClientResponse cr) {
        if (cr.statusCode().is4xxClientError()) {
            throw new RuntimeException("Rewrite client!");
        }
        if (cr.statusCode().is5xxServerError()) {
            throw new RuntimeException("Server fucked up!");
        }
        if (cr.statusCode().value() == 201) {
            log.debug("Succeeded post: {}.", cr.headers().asHttpHeaders().getLocation());
        }
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

}
