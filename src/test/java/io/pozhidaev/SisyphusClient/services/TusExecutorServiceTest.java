package io.pozhidaev.SisyphusClient.services;

import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.Function;

@RunWith(SpringRunner.class)
public class TusExecutorServiceTest {

    @MockBean
    TusClient tusClient;

    @MockBean
    Function<Path, TusUpload> tusUploadConsumer;


    @Test
    public void setTusClient() {
        final TusExecutorService tusExecutorService = new TusExecutorService();
        tusExecutorService.setTusClient(tusClient);
    }

    @Test
    public void setTusUploadConsumer() {
        final TusExecutorService tusExecutorService = new TusExecutorService();
        tusExecutorService.setTusUploadConsumer(tusUploadConsumer);
    }

    @Test
    public void load() throws IOException, ProtocolException {

        final Path tempFile = Files.createTempFile("TusExecutorServiceTest", "load");

        final TusUpload upload = Mockito.mock(TusUpload.class);
        final TusUploader uploader = Mockito.mock(TusUploader.class);

        Mockito.when(tusClient.getHeaders())
            .thenReturn(new HashMap<>());
        Mockito.when(tusUploadConsumer.apply(tempFile))
            .thenReturn(upload);

        Mockito.when(tusClient.resumeOrCreateUpload(upload))
            .thenReturn(uploader);

        Mockito.doNothing().when(uploader).setChunkSize(1024);

        Mockito.when(uploader.uploadChunk()).thenReturn(-1);
        Mockito.when(uploader.getUploadURL()).thenReturn(new URL("http://test/1"));

        final TusExecutorService tusExecutorService = new TusExecutorService();
        tusExecutorService.setTusClient(tusClient);
        tusExecutorService.setTusUploadConsumer(tusUploadConsumer);
        tusExecutorService.load(tempFile);
    }

    @Test
    public void makeAttempt() {
    }
}