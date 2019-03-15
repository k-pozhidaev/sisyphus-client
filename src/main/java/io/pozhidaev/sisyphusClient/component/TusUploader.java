package io.pozhidaev.sisyphusClient.component;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.file.StandardOpenOption.READ;


@Slf4j
@Service
public class TusUploader implements ApplicationRunner {

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

    @Override
    public void run(ApplicationArguments args) throws Exception {
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
//                .doOnNext(TusUploader::handleResponse)
//            )
        final Path path = Paths.get("/Users/i337731/.v8flags.5.5.372.42.i337731.json");


        final Mono<URI> startUploadMono = webClientFactoryMethod.get().post()
            .headers(httpHeaders -> {
                httpHeaders.set("Upload-Length", readFileSizeQuietly(path).toString());
                httpHeaders.set("Upload-Metadata", generateMetadataQuietly(path));
                httpHeaders.set("Mime-Type", readContentTypeQuietly(path));
            })
            .exchange()
            .doOnNext(this::handleResponse)
            .map(cr -> cr.headers().asHttpHeaders().getLocation());

        Mono.zip(startUploadMono, Mono.just(path))
            .map(t -> Tuples.of(t.getT1(), t.getT2(), t.getT2()))
            .map(t -> t.mapT3(p -> IntStream.range(0, getChunkCount(p))))
            .flatMap(t -> t.getT3().mapToObj(o -> uploadChunk(o, t.getT1(), t.getT2())).reduce(Mono::then).orElse(Mono.empty()))
//            .flatMap(t -> uploadChunk(0, t.getT1(), t.getT2()))
//            //TODO debug code
            .subscribe()
        ;


//        uploadChunk(0, URI.create("http://localhost:8080/upload/21"), path).subscribe();
//        dataBufferFlux(0, path).doOnNext(dataBuffer -> log.info("SIZE: {}", dataBuffer.capacity())).subscribe();
//        log.info("test");
    }

    int getChunkCount(final Path path) {
        return Long.valueOf(readFileSizeQuietly(path) / chunkSize.get()).intValue() + 1;
    }

    Mono<Long> uploadChunk(final long chunk, final URI uri, final Path path) {

        final int chunkSize = this.chunkSize.get();
        final long offset = chunk * chunkSize;
        final long tailSize = readFileSizeQuietly(path) - offset;
        final int contentLength = tailSize < chunkSize ? (int) tailSize : chunkSize;

        return webClientFactoryMethod.get()
            .patch()
            .uri(uri)
            .body(dataBufferFlux(chunk, path), DataBuffer.class)
            .header("Upload-Offset", Objects.toString(offset))
            .header("Content-Length", Objects.toString(contentLength))
            .header("Content-Type", "application/offset+octet-stream")
            .exchange()
            .doOnNext(cr -> log.info("Status: {}, chunk {} ", cr.rawStatusCode(), chunk))
            .map(ClientResponse::headers)
            .map(h -> h.header("Upload-Offset"))
            .map(Collection::stream)
            .map(s -> s.findFirst().orElseThrow(TusNotComplientExeption::new))
            .map(Long::valueOf)
            ;
    }

    Flux<DataBuffer> dataBufferFlux(final long chunk, final Path path) {
        final int chunkSize = this.chunkSize.get();
        final long offset = chunk * chunkSize;
        final AsynchronousFileChannel channel = asynchronousFileChannelQuietly(path);
        return DataBufferUtils.takeUntilByteCount(
            DataBufferUtils.readAsynchronousFileChannel(() -> channel, offset, new DefaultDataBufferFactory(), 256),
            chunkSize
        );
    }

    Mono<Tuple2<Integer, Long>> pathToBuffer(final long chunk, final Path path) {
        final int chunkSize = this.chunkSize.get();
        final long offset = chunk * chunkSize;
        final long tailSize = readFileSizeQuietly(path) - offset;

        log.info(
            "offset {}, tailSize {}, readFileSizeQuietly(path) {}",
            offset,
            tailSize,
            readFileSizeQuietly(path)
        );


        return Mono.zip(
            Mono.fromSupplier(() -> tailSize < chunkSize ? (int) tailSize : chunkSize),
            Mono.just(offset)
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
        return String.format(
                "%s-%s-%d",
                path.toAbsolutePath(),
                readFileSizeQuietly(path),
                readLastModifiedQuietly(path)
        );
    }

    long readLastModifiedQuietly(final Path path){
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            final RuntimeException exception = new RuntimeException("", e);
            log.error("Reading content type error.", exception);
            throw exception;
        }
    }

    private void handleResponse(final ClientResponse cr) {
        if (cr.statusCode().is4xxClientError()) {
            handleError(cr, new RuntimeException("Rewrite client!"));

        }
        if (cr.statusCode().is5xxServerError()) {
            handleError(cr, new RuntimeException("Server fucked up!"));
        }
        if (cr.statusCode().value() == 201) {
            log.debug("Succeeded post: {}.", cr.headers().asHttpHeaders().getLocation());
        }
    }

    private void handleError(final ClientResponse cr, final RuntimeException error) {
        log.error("TUS error response:", error);
        cr.headers().asHttpHeaders().forEach((s, strings) ->
            log.info("Header {}, value {}.", s, Strings.join(strings, ',')));
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
