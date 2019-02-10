package io.pozhidaev.SisyphusClient.services;

import io.tus.java.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@Service
public class TusExecutorService extends TusExecutor {

    private TusClient tusClient;
    private Function<Path, TusUpload> tusUploadConsumer;
    private TusUpload upload;
    private Path path;


    @Autowired
    public void setTusClient(final TusClient tusClient) {
        this.tusClient = tusClient;
    }

    @Autowired
    public void setTusUploadConsumer(final Function<Path, TusUpload> tusUploadConsumer) {
        this.tusUploadConsumer = tusUploadConsumer;
    }

    public void load(final Path path) throws ProtocolException, IOException  {
        this.path = path;
        upload = tusUploadConsumer.apply(path);
        makeAttempts();
    }

    @Override
    protected void makeAttempt() throws ProtocolException, IOException {

        log.debug(Files.probeContentType(path));
        Objects.requireNonNull(tusClient.getHeaders())
                .put("Mime-Type", Files.probeContentType(path));
        TusUploader uploader = tusClient.resumeOrCreateUpload(upload);
        uploader.setChunkSize(1024);
        do {
            long totalBytes = upload.getSize();
            long bytesUploaded = uploader.getOffset();
            double progress = (double) bytesUploaded / totalBytes * 100;

        } while (uploader.uploadChunk() > -1);

        uploader.finish();

        log.debug("Upload finished.");
        log.debug("Upload available at: {}", uploader.getUploadURL().toString());
    }
}
