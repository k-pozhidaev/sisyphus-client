package io.pozhidaev.sisyphusClient.configuration;

import io.pozhidaev.sisyphusClient.component.TusdUpload;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.nio.file.Files.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.integration.file.FileReadingMessageSource.WatchEventType.CREATE;
import static org.springframework.integration.file.FileReadingMessageSource.WatchEventType.MODIFY;
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
    private Integer[] intervals;

    @Bean
    TusdUpload.Builder tusdUploadBuilder (Supplier<WebClient> webClientFactoryMethod) {
        Assert.isTrue(intervals.length != 0, () -> "sisyphus-client.intervals cannot be empty");
        return TusdUpload.builder()
            .intervals(intervals)
            .client(webClientFactoryMethod.get());
    }

    @Bean
    Supplier<WebClient> webClientFactoryMethod() {
        return () -> WebClient
            .builder()
            .baseUrl(getUrl())
            .defaultHeaders(httpHeaders -> httpHeaders.add("X-Token", token))
            .build();
    }

    @Bean
    Path sourceFolder() {
        return pathFromStringParam(sourceFolder);
    }

    @Bean
    Supplier<Integer> chunkSize() {
        return () -> Objects.requireNonNull(chunkSize);
    }

    @Bean
    SubscribableChannel logChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    IntegrationFlow fileFlow() {
        return IntegrationFlows
            .from(
                inboundAdapter(sourceFolder().toFile())
                    .useWatchService(true)
                    .watchEvents(CREATE,MODIFY)
                    .filter(this::filterFiles),
                poller -> poller.poller(pm -> pm.fixedRate(1, SECONDS))
            )
            .channel(this.logChannel())
            .get();
    }

    List<File> filterFiles(final File[] files){
        return Arrays.stream(files)
            .filter(file -> System.currentTimeMillis() - file.lastModified() < 60_000)
            .collect(Collectors.toList());
    }

    @Bean
    Flux<Path> createdFileStream() {
        final SubscribableChannel subscribableChannel = logChannel();
        return Flux.create((Consumer<FluxSink<Path>>) fluxSink -> {
            final ForwardingMessageHandler handler = new ForwardingMessageHandler(fluxSink);
            subscribableChannel.subscribe(handler);
        })
            .onErrorResume(Exception.class, Flux::error);
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

        ForwardingMessageHandler(final FluxSink<Path> sink) {
            this.sink = sink;
        }

        @Override
        public void handleMessage(final Message<?> message) throws MessagingException {
            final File strPayloadFromChannel = (File) message.getPayload();
            sink.next(Paths.get(strPayloadFromChannel.getAbsolutePath()));
        }
    }

}
