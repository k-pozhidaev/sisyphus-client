package io.pozhidaev.sisyphusClient.component;

import io.pozhidaev.sisyphusClient.domain.*;
import io.pozhidaev.sisyphusClient.domain.exceptions.*;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
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
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.file.StandardOpenOption.READ;
import static java.util.Objects.requireNonNull;

@Slf4j
@Builder(builderClassName = "Builder")
@ToString
@EqualsAndHashCode
public class TusdUpload {

    private WebClient client;
    private Path path;
    private Integer chunkSize;
    private Integer[] intervals;
    private Options options;
    private FileStorage fileStorage;

    private URI patchUri;
    private long lastChunkUploaded;
    private long uploadedLength;


    Mono<TusdUpload> post(){
        return doPost()
                .doOnNext(this::handleResponse)
                .doOnNext(cr -> patchUri = requireNonNull(cr.headers().asHttpHeaders().getLocation()))
                .then(Mono.just(this))
            ;
    }

    Mono<ClientResponse> doPost(){
        return client.post()
            .headers(httpHeaders -> {
                httpHeaders.set("Upload-Length", Objects.toString(readFileSizeQuietly()));
                httpHeaders.set("Upload-Metadata", generateMetadataQuietly());
                httpHeaders.set("Mime-Type", readContentTypeQuietly());
            })
            .exchange();
    }

    Mono<Long> patchChain(){
        return IntStream.range(0, calcChunkCount())
            .mapToObj(this::patch)
            .reduce(Mono::then)
            .orElse(Mono.empty());
    }

    Mono<Long> patch(final Integer chunk){
        return patch(chunk, (byte) 0)
            .map(r -> uploadedLength += uploadedLengthFromResponse(r));
    }

    Mono<ClientResponse> patch(final Integer chunk, final byte attempt){
        final long offset = chunk * chunkSize;
        final long tailSize = readFileSizeQuietly() - offset;
        return client
            .patch()
            .uri(patchUri)
            .body(dataBufferFlux(chunk), DataBuffer.class)
            .header("Upload-Offset", Objects.toString(offset))
            .header("Content-Length", Objects.toString(Math.min(tailSize, chunkSize)))
            .header("Content-Type", "application/offset+octet-stream")
            .exchange()
            .doOnNext(cr -> log.debug("Status: {}, chunk {} ", cr.rawStatusCode(), chunk))
            .flatMap(r -> {
                if (!r.statusCode().isError()) return Mono.just(r);
                final byte nextAttempt = (byte) (attempt + 1);
                if (intervals.length == attempt) throw new FileUploadException(r);
                return Mono.delay(Duration.ofMillis(intervals[attempt])).then(patch(chunk, nextAttempt));
            })
            ;
    }


    long uploadedLengthFromResponse(final ClientResponse response) {
        return response.headers()
            .header("Upload-Offset")
            .stream()
            .findFirst()
            .map(Long::valueOf)
            .orElseThrow(() -> new FileUploadException(response));
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
            final FileUploadException exception = new FileUploadException(cr);
            log.error("TUS error response:", exception);
            throw exception;
        }
        log.debug("Succeeded post: {}.", cr.headers().asHttpHeaders().getLocation());
    }

    int calcChunkCount() {
        return Long.valueOf(readFileSizeQuietly() / chunkSize).intValue() + 1;
    }

    long readFileSizeQuietly() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            final FileSizeReadException exception = new FileSizeReadException(path, e);
            log.error("Reading file size error.", exception);
            throw exception;
        }
    }

    String calcFingerprint() {
        return String.format(
                "%s-%s",
                path.toAbsolutePath(),
                readFileSizeQuietly()
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
            final FileListModifiedReadException exception = new FileListModifiedReadException(path, e);
            log.error("Reading content type error.", exception);
            throw exception;
        }
    }

    String readContentTypeQuietly() {
        try {
            return probeContentType();
        } catch (IOException e) {
            final FileContentTypeReadException exception = new FileContentTypeReadException(path, e);
            log.error("Reading content type error.", exception);
            throw exception;

        }
    }

    AsynchronousFileChannel asynchronousFileChannelQuietly() {
        try {
            return AsynchronousFileChannel.open(path, READ);
        } catch (IOException e) {
            final AsynchronousFileChannelOpenException exception = new AsynchronousFileChannelOpenException(path, e);
            log.error("File read error.", exception);
            throw exception;
        }
    }

    String probeContentType() throws IOException {
        return Files.probeContentType(path);
    }

}
