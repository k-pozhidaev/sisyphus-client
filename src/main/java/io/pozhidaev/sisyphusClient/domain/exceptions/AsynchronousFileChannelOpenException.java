package io.pozhidaev.sisyphusClient.domain.exceptions;

import java.nio.file.Path;

public class AsynchronousFileChannelOpenException extends RuntimeException {
    public AsynchronousFileChannelOpenException(final Path path, final Throwable cause) {
        super(String.format("File content read exception: %s", path.toAbsolutePath()), cause);
    }
}
