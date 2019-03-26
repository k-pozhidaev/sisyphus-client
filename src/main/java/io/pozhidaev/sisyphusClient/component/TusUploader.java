package io.pozhidaev.sisyphusClient.component;

import io.pozhidaev.sisyphusClient.domain.Options;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

import static io.pozhidaev.sisyphusClient.domain.Options.getZeroElementIfExists;


@Slf4j
@Service
public class TusUploader implements ApplicationRunner {

    private Options options;

    private Flux<Path> createdFileStream;
    private Supplier<WebClient> webClientFactoryMethod;
    private Supplier<Integer> chunkSize;
    private TusdUpload.Builder tusdUploadBuilder;

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
    public void tusdUploadBuilder(final TusdUpload.Builder tusdUploadBuilder) {
        this.tusdUploadBuilder = tusdUploadBuilder;
    }

    @Override
    public void run(ApplicationArguments args) {

        webClientFactoryMethod
            .get()
            .options()
            .exchange()
            .map(ClientResponse::headers)
            .map(this::buildOptions)
            .doOnNext(o -> this.options = o)
            .thenMany(createdFileStream)
            .map(path -> tusdUploadBuilder
                .path(path)
                .options(options)
                .client(webClientFactoryMethod.get())
                .chunkSize(chunkSize.get())
                .build())
            .flatMap(TusdUpload::post)
            .flatMap(TusdUpload::patchChain)
            .subscribe()
        ;
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

}
