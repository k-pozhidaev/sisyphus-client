package io.pozhidaev.sisyphusClient.component;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.file.StandardOpenOption.READ;

@Slf4j
@Builder
public class TusdUpload {

    private WebClient client;
    private Path path;
    private Integer chunkSize;


    public void setFile(final Path path) {
        this.path = path;
    }

    public void setClient(WebClient client) {
        this.client = client;
    }


    public Mono<ClientResponse> post(){
        return client.post()
                .headers(httpHeaders -> {
                    httpHeaders.set("Upload-Length", Objects.toString(readFileSizeQuietly()));
                    httpHeaders.set("Upload-Metadata", generateMetadataQuietly());
                    httpHeaders.set("Mime-Type", readContentTypeQuietly());
                })
                .exchange()
                .doOnNext(this::handleResponse);
    }

    public Mono<Long> patchChain(final URI patchUri){
        return IntStream.range(0, calcChunkCount())
            .mapToObj(chunk -> patch(chunk, patchUri))
            .reduce(Mono::then)
            .orElse(Mono.empty());
    }

    public Mono<Long> patch(final Integer chunk, final URI patchUri){
        final long offset = chunk * chunkSize;
        final long tailSize = readFileSizeQuietly() - offset;
        final int contentLength = tailSize < chunkSize ? (int) tailSize : chunkSize;

        final Mono<Long> patch = client
            .patch()
            .uri(patchUri)
            .body(dataBufferFlux(chunk), DataBuffer.class)
            .header("Upload-Offset", Objects.toString(offset))
            .header("Content-Length", Objects.toString(contentLength))
            .header("Content-Type", "application/offset+octet-stream")
            .exchange()
            .doOnNext(cr -> log.info("Status: {}, chunk {} ", cr.rawStatusCode(), chunk))
            .map(ClientResponse::headers)
            .map(h -> h.header("Upload-Offset"))
            .map(Collection::stream)
            .map(s -> s.findFirst().orElseThrow(TusUploader.TusNotComplientExeption::new))
            .map(Long::valueOf);
//        patch.retryWhen(throwableFlux -> {})
        return patch;
    }


    Flux<DataBuffer> dataBufferFlux(final long chunk) {
        final long offset = chunk * chunkSize;
        final AsynchronousFileChannel channel = asynchronousFileChannelQuietly();
        return DataBufferUtils.takeUntilByteCount(
                DataBufferUtils.readAsynchronousFileChannel(() -> channel, offset, new DefaultDataBufferFactory(), 256),
                chunkSize
        );
    }

     void handleResponse(final ClientResponse cr) {
        if (cr.statusCode().isError()) {
            handleError(cr, cr.statusCode().getReasonPhrase());
        }
        log.debug("Succeeded post: {}.", cr.headers().asHttpHeaders().getLocation());
    }

    void handleError(final ClientResponse cr, final String errorMessage) {
        final RuntimeException exception = new RuntimeException(errorMessage);
        log.error("TUS error response:", exception);
        cr.headers().asHttpHeaders().forEach((headerKey, header) -> log.info(
                "Header {}, value {}.",
                headerKey,
                Strings.join(header, ',')
        ));
        throw exception;
    }

    int calcChunkCount() {
        return Long.valueOf(readFileSizeQuietly() / chunkSize).intValue() + 1;
    }

    long readFileSizeQuietly() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            final TusUploader.FileSizeReadException exception = new TusUploader.FileSizeReadException(path, e);
            log.error("Reading file size error.", exception);
            throw exception;
        }
    }

    String calcFingerprint() {
        return String.format(
                "%s-%s-%d",
                path.toAbsolutePath(),
                readFileSizeQuietly(),
                readLastModifiedQuietly()
        );
    }

    String generateMetadataQuietly() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("filename", path.getFileName().toString());
        metadata.put("fingerprint", calcFingerprint());

        return metadata
                .entrySet()
                .stream()
                .map(e -> String.format("%s %s", e.getKey(), Base64.getEncoder().encodeToString(e.getValue().getBytes())))
                .collect(Collectors.joining(","))
                ;
    }

    long readLastModifiedQuietly(){
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            final RuntimeException exception = new RuntimeException("", e);
            log.error("Reading content type error.", exception);
            throw exception;
        }
    }

    String readContentTypeQuietly() {
        try {
            return Files.probeContentType(path);
        } catch (IOException e) {
            final TusUploader.FileContentTypeReadException exception = new TusUploader.FileContentTypeReadException(path, e);
            log.error("Reading content type error.", exception);
            throw exception;

        }
    }

    AsynchronousFileChannel asynchronousFileChannelQuietly() {
        try {
            return AsynchronousFileChannel.open(path, READ);
        } catch (IOException e) {
            final TusUploader.AsynchronousFileChannelOpenException exception = new TusUploader.AsynchronousFileChannelOpenException(path, e);
            log.error("File read error.", exception);
            throw exception;
        }
    }

}
