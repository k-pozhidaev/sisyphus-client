package io.pozhidaev.SisyphusClient.services;

import io.tus.java.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
public class TusExecutorService extends TusExecutor {

    private TusClient tusClient;
    private Function<Path, TusUpload> tusUploadConsumer;
    private TusUpload upload;
    private Path path;
    private Supplier<Integer> chunkSize;

    @Autowired
    public void setTusClient(final TusClient tusClient) {
        this.tusClient = tusClient;
    }

    @Autowired
    public void setTusUploadConsumer(final Function<Path, TusUpload> tusUploadConsumer) {
        this.tusUploadConsumer = tusUploadConsumer;
    }

    @Autowired
    public void setChunkSize(final Supplier<Integer> chunkSize) {
        this.chunkSize = chunkSize;
    }



    public void load(final Path path) throws ProtocolException, IOException  {
        this.path = path;
        upload = tusUploadConsumer.apply(path);
        makeAttempts();
    }

    @Override
    protected void makeAttempt() throws ProtocolException, IOException {

        log.debug(Files.probeContentType(path));
        final Map<String, String> headers = Objects.requireNonNull(tusClient.getHeaders());
        headers.put("Mime-Type", Files.probeContentType(path));
        headers.put("Content-Length", "10000");
        tusClient.setHeaders(headers);
        TusUploader uploader = tusClient.resumeOrCreateUpload(upload);
        uploader.setChunkSize(chunkSize.get());
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
