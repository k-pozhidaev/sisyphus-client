package io.pozhidaev.sisyphusClient.component;

import io.pozhidaev.sisyphusClient.domain.FileStorage;
import io.pozhidaev.sisyphusClient.domain.Options;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.function.Supplier;


@Slf4j
@Service
public class TusUploader implements ApplicationRunner {

    private Options options;

    private Flux<Path> createdFileStream;
    private Supplier<WebClient> webClientFactoryMethod;
    private Supplier<Integer> chunkSize;
    private TusdUpload.Builder tusdUploadBuilder;
    private FileStorage fileStorage;

    @Autowired
    public void setCreatedFileStream(final Flux<Path> createdFileStream) {
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

    @Autowired
    public void setFileStorage(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }


    @Override
    public void run(final ApplicationArguments args) {
        options()
            .map(ClientResponse::headers)
            .map(Options::buildOptions)
            .doOnNext(o -> options = o)
            .thenMany(createdFileStream)
            .map(this::tusdUploadBuild)
            .flatMap(TusdUpload::post)
            .flatMap(TusdUpload::patchChain)
            .subscribe()
        ;
    }

    Mono<ClientResponse> options(){
        return webClientFactoryMethod
            .get()
            .options()
            .exchange();
    }

    TusdUpload tusdUploadBuild(final Path path){
        return tusdUploadBuilder
            .path(path)
            .options(options)
            .fileStorage(fileStorage)
            .client(webClientFactoryMethod.get())
            .chunkSize(chunkSize.get())
            .build();
    }

}
