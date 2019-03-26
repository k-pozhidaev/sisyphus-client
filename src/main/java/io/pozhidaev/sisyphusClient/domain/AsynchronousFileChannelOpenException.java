package io.pozhidaev.sisyphusClient.domain;

import java.nio.file.Path;

public class AsynchronousFileChannelOpenException extends RuntimeException {
    public AsynchronousFileChannelOpenException(Path path, Throwable cause) {
        super(String.format("File content read exception: %s", path.toAbsolutePath()), cause);
    }
}
