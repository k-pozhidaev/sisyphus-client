package io.pozhidaev.sisyphusClient.component;

import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TusUpload implements Upload {

    private WebClient client;
    private Path path;

    @Override
    public void setFile(final Path path) {
        this.path = path;
    }

    public void setClient(WebClient client) {
        this.client = client;
    }

    @Override
    public void start() {
    }

    protected String calcFingerprint()  {
        try {
            final long size = Files.size(path);
            return String.format("%s-%d", path.toAbsolutePath(), size);
        } catch (IOException e) {
            throw new RuntimeException("File size calculation exception", e);
        }
    }

    protected void head(String fingerprint){


    }

    protected void post(){
        patch("");
    }

//    protected void post(String fingerprint){}

    protected void patch(String fingerprint){}

}
