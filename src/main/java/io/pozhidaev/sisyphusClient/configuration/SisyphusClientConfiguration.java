package io.pozhidaev.sisyphusClient.configuration;

import io.tus.java.client.TusClient;
import io.tus.java.client.TusURLMemoryStore;
import io.tus.java.client.TusUpload;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.nio.file.Files.*;
import static org.springframework.integration.file.dsl.Files.inboundAdapter;



@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sisyphus-client")
@EnableConfigurationProperties(SisyphusClientConfiguration.class)
public class SisyphusClientConfiguration {

    private String url;
    private String sourceFolder;
    private String completedFolder;
    private String token;
    private Integer chunkSize;


    @PostConstruct
    void onInit() throws IOException {
        if(completedFolder().equals(sourceFolder())){
            throw new IOException("Source and completed folders could not be equals");
        }
    }

    @Bean
    WebClient webClient(){
        return WebClient
                .builder()
                .baseUrl(getUrl())
                .defaultHeaders(httpHeaders -> httpHeaders.add("X-Token", token))
                .build();
    }

    @Bean
    Path completedFolder() {
        return pathFromStringParam(completedFolder);
    }

    @Bean
    Path sourceFolder() {
        return pathFromStringParam(sourceFolder);
    }

    @Bean
    Supplier<Integer> chunkSize(){
        return () -> Objects.requireNonNull(chunkSize);
    }

    @Bean
    Function<Path, TusUpload> tusUploadConsumer(){
        return (final Path path) -> {
            try {
                log.debug("Uploading file: {}", path.toAbsolutePath());
                return new TusUpload(path.toFile());
            } catch (FileNotFoundException e) {
                log.error("Upload error");
                throw new RuntimeException("Upload error", e);
            }
        };
    }

    @Bean
    TusClient tusClient() throws MalformedURLException {

        final TusClient client = new TusClient();
        client.setUploadCreationURL(new URL(url));
        client.enableResuming(new TusURLMemoryStore());
        client.setHeaders(new HashMap<String, String>(){{
            put("X-Token", token);
        }});
        return client;
    }

    @Bean
    SubscribableChannel logChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    IntegrationFlow fileFlow() {
        return IntegrationFlows
                .from(inboundAdapter(
                        sourceFolder().toFile()).useWatchService(true),
                        poller -> poller.poller(pm -> pm.fixedRate(1000))
                )
                .channel(this.logChannel())
                .get();
    }


    @Bean
    CommandLineRunner c2(){
        return args -> {

            final SubscribableChannel subscribableChannel = logChannel();
            Flux.create((Consumer<FluxSink<Path>>) fluxSink -> {
                final ForwardingMessageHandler handler = new ForwardingMessageHandler(fluxSink);
                subscribableChannel.subscribe(handler);
            })
            .onErrorResume(Exception.class, Flux::error)
            .doOnNext(path -> log.info("path added: {}", path))
            .subscribe();
        };
    }

    private Path pathFromStringParam(final String folder) {

        Objects.requireNonNull(folder, "Source and Completed folders can not be null.");

        final Path writeDirectoryPath = Paths.get(folder);

        if (!exists(writeDirectoryPath)) {
            try {
                createDirectories(writeDirectoryPath);
            } catch (IOException e) {
                throw new RuntimeException("Directory create error.", e);
            }
        }

        log.info("Source files path: {}", writeDirectoryPath);

        if (!isWritable(writeDirectoryPath)) {
            throw new RuntimeException("Directory is not writable: " + folder);
        }
        return writeDirectoryPath;
    }


    class ForwardingMessageHandler implements MessageHandler {

        private final FluxSink<Path> sink;

        ForwardingMessageHandler(FluxSink<Path> sink) {
            this.sink = sink;
        }

        @Override
        public void handleMessage(Message<?> message) throws MessagingException {

            File strPayloadFromChannel = (File) message.getPayload();

            sink.next(Paths.get(strPayloadFromChannel.getAbsolutePath()));
        }
    }

}
