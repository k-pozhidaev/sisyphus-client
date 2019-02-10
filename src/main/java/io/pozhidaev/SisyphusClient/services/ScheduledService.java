package io.pozhidaev.SisyphusClient.services;

import io.tus.java.client.ProtocolException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Slf4j
@Service
public class ScheduledService {


    private final Path sourceFolder;

    private final Path completedFolder;

    private final TusExecutorService tusExecutorService;


    @Autowired
    public ScheduledService(
        @Qualifier("sourceFolder") Path sourceFolder,
        @Qualifier("completedFolder") Path completedFolder,
        TusExecutorService tusExecutorService
    ) {
        this.sourceFolder = sourceFolder;
        this.completedFolder = completedFolder;
        this.tusExecutorService = tusExecutorService;
    }


    @Scheduled(fixedDelay = 1000)
    void filesExistingTask() throws IOException {
        Files.walk(sourceFolder)
            .filter(e -> !e.equals(sourceFolder))
            .filter(this::isNotDirectory)
            .filter(this::isReadable)
            .forEach(this::loadToServer);
    }

    private void loadToServer(final Path path) {
        try {
            log.debug("{}",path);
            tusExecutorService.load(path);
            final Path arg = Paths.get(
                    completedFolder.toString(),
                    LocalDateTime.now().getNano() + "-" + path.getFileName().toString());
            Files.move(path, arg);
            log.info("Moving {} to {}", path, arg);
        } catch (ProtocolException|IOException e) {
            log.error("Error on files upload.");
        }
    }

    private boolean isReadable(final Path path) {
        final boolean isReadable = Files.isReadable(path);
        if (!isReadable) log.error("File is not readable: {}", path);
        return isReadable;
    }

    private boolean isNotDirectory(final Path path){
        final boolean isNotDirectory = !Files.isDirectory(path);
        if (!isNotDirectory) log.error("File is directory: {}", path);
        return isNotDirectory;
    }

}
