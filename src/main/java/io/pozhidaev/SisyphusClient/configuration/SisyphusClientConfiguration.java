package io.pozhidaev.SisyphusClient.configuration;

import io.tus.java.client.TusClient;
import io.tus.java.client.TusURLMemoryStore;
import io.tus.java.client.TusUpload;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

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

    @PostConstruct
    void onInit() throws IOException {
        if(completedFolder().equals(sourceFolder())){
            throw new IOException("Source and completed folders could not be equals");
        }
    }

    @Bean
    Path completedFolder() throws IOException {
        return pathFromStringParam(completedFolder);
    }

    @Bean
    Path sourceFolder() throws IOException {
        return pathFromStringParam(sourceFolder);
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

    private Path pathFromStringParam(final String folder) throws IOException {

        Objects.requireNonNull(folder, "Source and Completed folders can not be null.");

        final Path writeDirectoryPath = Paths.get(folder);

        if (!Files.exists(writeDirectoryPath)) {
            Files.createDirectories(writeDirectoryPath);
        }

        log.info("Source files path: {}", writeDirectoryPath);

        if (!Files.isWritable(writeDirectoryPath)) {
            throw new AccessDeniedException(folder);
        }
        return writeDirectoryPath;
    }

}
