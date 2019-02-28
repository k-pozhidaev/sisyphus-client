package io.pozhidaev.SisyphusClient.component;

import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TusUpload<T> implements Upload {

    private WebClient client;
    private Path path;

    @Override
    public void setFile(final Path path) {
        this.path = path;
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

    protected void head(T fingerprint){}

    protected void post(){}

    protected void post(T fingerprint){}

    protected void patch(T fingerprint){}

    protected void options(){}
}
