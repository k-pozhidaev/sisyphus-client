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
                    httpHeaders.set("Upload-Length", Objects.toString(readFileSizeQuietly(path)));
                    httpHeaders.set("Upload-Metadata", generateMetadataQuietly(path));
                    httpHeaders.set("Mime-Type", readContentTypeQuietly(path));
                })
                .exchange()
                .doOnNext(this::handleResponse);
    }


    public Mono<Long> patch(final Integer chunk, final URI patchUri){
        final long offset = chunk * chunkSize;
        final long tailSize = readFileSizeQuietly(path) - offset;
        final int contentLength = tailSize < chunkSize ? (int) tailSize : chunkSize;

        return client
                .patch()
                .uri(patchUri)
                .body(dataBufferFlux(chunk, path), DataBuffer.class)
                .header("Upload-Offset", Objects.toString(offset))
                .header("Content-Length", Objects.toString(contentLength))
                .header("Content-Type", "application/offset+octet-stream")
                .exchange()
                .doOnNext(cr -> log.info("Status: {}, chunk {} ", cr.rawStatusCode(), chunk))
                .map(ClientResponse::headers)
                .map(h -> h.header("Upload-Offset"))
                .map(Collection::stream)
                .map(s -> s.findFirst().orElseThrow(TusUploader.TusNotComplientExeption::new))
                .map(Long::valueOf)
                ;
    }


    Flux<DataBuffer> dataBufferFlux(final long chunk, final Path path) {
        final long offset = chunk * chunkSize;
        final AsynchronousFileChannel channel = asynchronousFileChannelQuietly(path);
        return DataBufferUtils.takeUntilByteCount(
                DataBufferUtils.readAsynchronousFileChannel(() -> channel, offset, new DefaultDataBufferFactory(), 256),
                chunkSize
        );
    }

     void handleResponse(final ClientResponse cr) {
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

    void handleError(final ClientResponse cr, final RuntimeException error) {
        log.error("TUS error response:", error);
        cr.headers().asHttpHeaders().forEach((headerKey, header) -> log.info(
                "Header {}, value {}.",
                headerKey,
                Strings.join(header, ',')
        ));
    }

    int getChunkCount(final Path path) {
        return Long.valueOf(readFileSizeQuietly(path) / chunkSize).intValue() + 1;
    }

    private static long readFileSizeQuietly(final Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            final TusUploader.FileSizeReadException exception = new TusUploader.FileSizeReadException(path, e);
            log.error("Reading file size error.", exception);
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

    long readLastModifiedQuietly(final Path path){
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            final RuntimeException exception = new RuntimeException("", e);
            log.error("Reading content type error.", exception);
            throw exception;
        }
    }

    private static String readContentTypeQuietly(final Path path) {
        try {
            return Files.probeContentType(path);
        } catch (IOException e) {
            final TusUploader.FileContentTypeReadException exception = new TusUploader.FileContentTypeReadException(path, e);
            log.error("Reading content type error.", exception);
            throw exception;

        }
    }

    AsynchronousFileChannel asynchronousFileChannelQuietly(final Path path) {
        try {
            return AsynchronousFileChannel.open(path, READ);
        } catch (IOException e) {
            final TusUploader.AsynchronousFileChannelOpenException exception = new TusUploader.AsynchronousFileChannelOpenException(path, e);
            log.error("File read error.", exception);
            throw exception;
        }
    }

}
