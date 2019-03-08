package io.pozhidaev.sisyphusClient.component;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Slf4j
@Service
public class TusUploader {

    private Options options;

    private final Flux<Path> createdFileStream;
    private final Supplier<WebClient> webClientFactoryMethod;

    @Autowired
    public TusUploader(final Supplier<WebClient> webClientFactoryMethod, final Flux<Path> createdFileStream) {
        this.createdFileStream = createdFileStream;
        this.webClientFactoryMethod = webClientFactoryMethod;
    }

//    @PostConstruct
    void onInit () {
        webClientFactoryMethod
            .get()
            .options()
            .exchange()
            .map(ClientResponse::headers)
            .map(this::buildOptions)
            .doOnNext(o -> this.options = o)
            .thenMany(createdFileStream)
            .flatMap(path -> webClientFactoryMethod.get().post()
                .headers(httpHeaders -> {
                    httpHeaders.set("Upload-Length", readFileSizeQuietly(path));
                    httpHeaders.set("Upload-Metadata", generateMetadataQuietly(path));
                    httpHeaders.set("Mime-Type", readContentTypeQuietly(path));
                })
                .exchange()
            )
            .map(clientResponse -> Objects.requireNonNull(clientResponse.headers().asHttpHeaders().getLocation()).getPath())
            .flatMap(s -> webClientFactoryMethod.get()
                .patch()
                .uri(u -> u.path(s).build())
                .exchange()
            )
        ;
    }


    private Options buildOptions(final ClientResponse.Headers headers){
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
    private class Options{
        public final static String HEADER_NAME_RESUMABLE = "Tus-Resumable";
        public final static String HEADER_NAME_VERSION = "Tus-Version";
        public final static String HEADER_NAME_EXTENSION = "Tus-Extension";
        public final static String HEADER_NAME_MAX_SIZE = "Tus-Max-Size";

        private String resumable;
        private List<String> version;
        private List<String> extension;
        private Long maxSize;

    }

    private static <T> Optional<T> getZeroElementIfExists(final List<T> list){
        if (list.size() > 0) {
            return Optional.of(list.get(0));
        }
        return Optional.empty();
    }

    private static String readFileSizeQuietly(final Path path) {
        try {
            return String.valueOf(Files.size(path));
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

        return metadata
            .entrySet()
            .stream()
            .map(e -> String.format("%s %s", e.getKey(), Base64.getEncoder().encodeToString(e.getValue().getBytes())))
            .collect(Collectors.joining(","))
        ;
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

}
